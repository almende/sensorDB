/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.agent;

import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.sensordb.storage.RedisStore;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Manager.
 */
@Access(AccessType.PUBLIC)
public class Manager extends Agent {
	private static final Logger	LOG		= Logger.getLogger(Manager.class
												.getName());

	/** The store. */
	private static RedisStore	store	= null;

	public void onReady() {
		ObjectNode redisConfig = (ObjectNode) getConfig().get("redis");
		if (redisConfig == null) {
			LOG.severe("Redis configuration for Sensor store is missing from Manager agent configuration");
		} else {
			store = new RedisStore(redisConfig);
		}
	}

	/**
	 * Gets the sensor store.
	 *
	 * @return the sensor store
	 */
	public static RedisStore getSensorStore() {
		if (store == null) {
			LOG.severe("Sensor store in the Manager agent is not (yet) initialized.");
		}
		return store;
	}
	// Setup demo import
}
