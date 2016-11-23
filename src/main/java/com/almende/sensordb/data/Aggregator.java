/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * The Class Aggregator.
 */
public class Aggregator {
	static Map<String, Aggregator>	aggregators	= new HashMap<String, Aggregator>();
	static {
		aggregators.put("average", new Average());
	}

	/**
	 * Gets the.
	 *
	 * @param name
	 *            the name
	 * @return the aggregator
	 */
	public static Aggregator get(String name) {
		return aggregators.get(name);
	}

	private String	name	= "";

	/**
	 * Instantiates a new aggregator.
	 *
	 * @param name
	 *            the name
	 */
	public Aggregator(String name) {
		this.name = name;
	}

	/**
	 * Aggregate.
	 *
	 * @param data
	 *            the data
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return the object node
	 */
	public Double aggregate(ArrayNode data, double start, double end) {
		return Double.NaN;
	}

	/**
	 * Aggregate.
	 *
	 * @param data
	 *            the data
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return the object node
	 */
	public Double aggregate2(ArrayNode data, double start, double end) {
		return Double.NaN;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

}

class Average extends Aggregator {

	public Average() {
		super("Average");
	}

	@Override
	public Double aggregate(ArrayNode values, double start, double end) {
		double result = 0.0;
		double prev = start;
		double val = 0.0;
		for (JsonNode item : values) {
			result += (val * (item.get("timestamp").asLong() - prev));
			if (item.get("value") != null) {
				val = item.get("value").asDouble();
				prev = item.get("timestamp").asLong();
				if (prev < start) {
					prev = start;
				}
			}
		}
		result += (val * (end - prev));
		return result / (end - start);
	}

	@Override
	public Double aggregate2(ArrayNode values, double start, double end) {
		Double result = Double.NaN;
		if (values.size() > 0) {
			double total = 0;
			Iterator<JsonNode> iter = values.elements();
			while (iter.hasNext()) {
				JsonNode item = iter.next();
				ArrayNode data = (ArrayNode) item.get("data");
				total += aggregate(data, start, end);
			}
			result = new Double(total / values.size());
		}
		return result;
	}
}
