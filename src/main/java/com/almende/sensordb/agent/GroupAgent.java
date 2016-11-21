/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.agent;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.sensordb.data.Aggregator;
import com.almende.util.TypeUtil;
import com.almende.util.URIUtil;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class GroupAgent.
 */
@Access(AccessType.PUBLIC)
public class GroupAgent extends SensorAgent {
	private static final Logger	LOG	= Logger.getLogger(GroupAgent.class
											.getName());

	// Group membership
	/**
	 * Adds the membership.
	 *
	 * @param member
	 *            the member
	 */
	public void addMembership(@Name("member") URI member) {
		final List<String> memberIds = getMembers();
		if (!memberIds.contains(member.toASCIIString())) {
			memberIds.add(member.toASCIIString());
			getState().put("memberIds", memberIds);
		}
	}

	/**
	 * Gets the members.
	 *
	 * @return the members
	 */
	public List<String> getMembers() {
		List<String> memberIds = getState().get("memberIds",
				new TypeUtil<ArrayList<String>>() {});
		if (memberIds == null) {
			memberIds = new ArrayList<String>();
		}
		return memberIds;
	}

	// Shared aggregated data
	// scheduled update of aggregated data from members (also doing push from
	// members to group?)
	// Interval
	static int	interval	= 1;	// Minutes

	/*
	 * (non-Javadoc)
	 * @see com.almende.sensordb.agent.SensorAgent#onReady()
	 */
	public void onReady() {
		super.onReady();
		final Params params = new Params();
		params.add("sensorId", "HeartRate");
		params.add("aggregator", "average");
		this.scheduleInterval("updateAggregatedData", params, 60000);
	}

	/**
	 * Update aggregated data.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param aggregator
	 *            the aggregator
	 */
	public void updateAggregatedData(@Name("sensorId") String sensorId,
			@Name("aggregator") String aggregator) {
		// Collect data from members for timeslot
		// TODO: Make Async
		List<ObjectNode> data = new ArrayList<ObjectNode>();
		final Params params = new Params();
		params.add("sensorId", sensorId);
		params.add("start", getScheduler().nowDateTime().minusMinutes(interval));
		params.add("end", getScheduler().now());

		for (String member : getMembers()) {
			final URI mem = URIUtil.create(member);
			ArrayNode res;
			try {
				res = getCaller().callSync(mem, "getSensorValues", params,
						ArrayNode.class);
				final ObjectNode result = JOM.createObjectNode();
				result.set("data", res);
				result.put("sensorId", sensorId);
				result.put("user", member);
				data.add(result);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Failed to get SensorValues", e);
			}
		}
		// Run aggregator with data
		setSensorValue(sensorId, params.get("end").asLong(),
				Aggregator.get(aggregator).aggregate(data, params),
				this.getUrlByScheme("local"));
	}

	// Selection pattern

}
