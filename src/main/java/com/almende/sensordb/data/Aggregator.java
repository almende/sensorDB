/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Aggregator.
 */
public class Aggregator {
	static Map<String, Aggregator>	aggregators	= new HashMap<String, Aggregator>();
	static {
		aggregators.put("average", new Avarage());
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
	 * @param params
	 *            the params
	 * @return the object node
	 */
	public String aggregate(List<ObjectNode> data, Params params) {
		return "";
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

class Avarage extends Aggregator {

	public Avarage() {
		super("Avarage");
	}

	public double getAverage(ArrayNode values,long start,long end){
		double result = 0.0;
		long prev=  start;
		
		
		return result;
	}
	public String aggregate(List<ObjectNode> data, Params params) {
		String result="";
		if (data.size() > 0) {
			double total = 0;
			for (ObjectNode item : data) {
				ArrayNode values = (ArrayNode) item.get("data");
				//
				total += getAverage(values,params.get("start").asLong(),params.get("end").asLong());
			}
			result = new Double(total / data.size()).toString();
		}
		return result;
	}
}
