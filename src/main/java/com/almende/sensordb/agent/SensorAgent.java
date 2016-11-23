/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.agent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Tuple;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.sensordb.data.Aggregator;
import com.almende.sensordb.storage.RedisStore;
import com.almende.util.TypeUtil;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SensorAgent.
 */
@Access(AccessType.PUBLIC)
public class SensorAgent extends Agent {
	private RedisStore	sensorStore	= null;

	public void onReady() {
		sensorStore = new RedisStore(getConfig());
	}

	/**
	 * Sets the sensor value.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param timestamp
	 *            the timestamp
	 * @param value
	 *            the value
	 * @param sender
	 *            the sender
	 */
	public void setSensorValue(@Name("sensorId") String sensorId,
			@Name("timestamp") long timestamp, @Name("value") String value,
			@Sender URI sender) {

		final List<String> sensorIds = getSensorIds(sender);
		if (!sensorIds.contains(sensorId)) {
			sensorIds.add(sensorId);
			getState().put("sensorIds", sensorIds);
		}

		sensorStore.storeSensorValue(getId() + ":sensor:" + sensorId, value,
				timestamp);
	}

	/**
	 * Gets the sensor values.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param inclPrev
	 *            the incl prev
	 * @param sender
	 *            the sender
	 * @return the sensor values
	 */
	public ArrayNode getSensorValues(@Name("sensorId") String sensorId,
			@Name("start") double start, @Name("end") double end,
			@Name("includePrevious") @Optional Boolean inclPrev,
			@Sender URI sender) {
		final Set<Tuple> res = sensorStore.getValues(getId() + ":sensor:"
				+ sensorId, start, end, inclPrev);
		final ArrayNode result = JOM.createArrayNode();
		for (Tuple tuple : res) {
			final ObjectNode node = JOM.createObjectNode();
			node.put("timestamp", tuple.getScore());
			node.put("value", tuple.getElement());
			result.add(node);
		}
		return result;
	}

	/**
	 * Gets the sensor ids.
	 *
	 * @param sender
	 *            the sender
	 * @return the sensor ids
	 */
	public List<String> getSensorIds(@Sender URI sender) {
		List<String> sensorIds = getState().get("sensorIds",
				new TypeUtil<ArrayList<String>>() {});
		if (sensorIds == null) {
			sensorIds = new ArrayList<String>();
		}
		return sensorIds;
	}

	/**
	 * Gets the graph json.
	 *
	 * @param sender
	 *            the sender
	 * @return the graph json
	 */
	public ObjectNode getGraphData(@Sender URI sender) {
		final ObjectNode result = JOM.createObjectNode();
		for (String sensor : getSensorIds(sender)) {
			final ObjectNode wrapper = JOM.createObjectNode();
			wrapper.set(
					"values",
					getSensorValues(sensor, 0, Double.POSITIVE_INFINITY, false,
							sender));
			wrapper.put("label", sensor);
			wrapper.put("name", getId());
			result.set(sensor, wrapper);
		}
		return result;
	}

	/**
	 * Gets the pattern.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param aggregator
	 *            the aggregator
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param steps
	 *            the steps
	 * @param sender
	 *            the sender
	 * @return the pattern
	 */
	// getHourly average
	public ObjectNode getPattern(@Name("sensorId") String sensorId,
			@Name("aggregator") String aggregator, @Name("start") double start,
			@Name("end") double end, @Name("steps") double steps,
			@Sender URI sender) {
		ObjectNode result = JOM.createObjectNode();
		result.put("sensorId", sensorId);
		result.put("aggregator", aggregator);
		result.put("start", start);
		result.put("end", end);
		result.put("steps", steps);

		final ArrayNode pattern = JOM.createArrayNode();
		final double inc = (end - start) / steps;
		for (int i = 0; i < steps; i++) {
			pattern.add(_getPattern(sensorId, aggregator, start + (i * inc),
					start + (i * inc) + inc, sender));
		}
		result.set("pattern", pattern);
		return result;
	}

	/**
	 * Gets the pattern.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param aggregator
	 *            the aggregator
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param sender
	 *            the sender
	 * @return the pattern
	 */
	private double _getPattern(String sensorId, String aggregator,
			double start, double end, URI sender) {
		final Params params = new Params();
		params.put("start", start);
		params.put("end", end);
		return Aggregator.get(aggregator)
				.aggregate(getSensorValues(sensorId, start, end, true, sender),
						start, end);
	}

	/**
	 * Distance.
	 *
	 * @param pattern
	 *            the pattern
	 * @param cutoff
	 *            the cutoff
	 * @param sender
	 *            the sender
	 * @return true, if successful
	 */
	public boolean closeBy(@Name("pattern") ObjectNode pattern,
			@Name("cutoff") double cutoff, @Sender URI sender) {
		ObjectNode myPattern = getPattern(pattern.get("sensorId").asText(),
				pattern.get("aggregator").asText(), pattern.get("start")
						.asDouble(), pattern.get("end").asDouble(), pattern
						.get("steps").asDouble(), sender);
		ArrayNode list = (ArrayNode)myPattern.get("pattern");
		double total = 0.0;
		for (int i=0; i<list.size(); i++){
			total += Math.abs(list.get(i).asDouble()-pattern.get("pattern").get(i).asDouble());
		}
		return (total/list.size())<=cutoff;
	}
	// Compare own hourly average with provided example -> KPI

}
