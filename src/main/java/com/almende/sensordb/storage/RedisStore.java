/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.storage;

import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Tuple;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class RedisStore.
 */
public class RedisStore {
	private final JedisPool	pool;
	private final int		id;

	/**
	 * Instantiates a new redis store.
	 *
	 * @param params
	 *            the params
	 */
	public RedisStore(final ObjectNode params) {
		pool = new JedisPool(new JedisPoolConfig(), params.get("pool").asText());
		id = params.get("dbid").asInt();
	}

	/**
	 * Gets the single jedis instance from the RedisStore.
	 *
	 * @return single jedis instance from the RedisStore
	 */
	private Jedis getInstance() {
		final Jedis res = pool.getResource();
		res.select(id);
		return res;
	}

	/**
	 * Store sensor value.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param timestamp
	 *            the timestamp
	 * @param value
	 *            the value
	 */
	public void storeSensorValue(String sensorId, String value, double timestamp){
		try (final Jedis instance = getInstance()) {
			instance.zadd(sensorId, timestamp, value);
		}
	}
	
	/**
	 * Store sensor values.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param values
	 *            the values, Map<Value, timestamp>
	 */
	public void storeSensorValues(String sensorId, Map<String,Double> values){
		try (final Jedis instance = getInstance()) {
			instance.zadd(sensorId,values);
		}
	}
	
	/**
	 * Gets the values.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param timestamp
	 *            the timestamp
	 * @return the values
	 */
	public Set<Tuple> getValues(String sensorId, double timestamp){
		try (final Jedis instance = getInstance()) {
			return instance.zrangeByScoreWithScores(sensorId, timestamp, timestamp);
		}
	}

	/**
	 * Gets the values.
	 *
	 * @param sensorId
	 *            the sensor id
	 * @param from
	 *            the from timestamp (included)
	 * @param to
	 *            the to timestamp (included!)
	 * @return the values
	 */
	public Set<Tuple> getValues(String sensorId, double from, double to){
		try (final Jedis instance = getInstance()) {
			return instance.zrangeByScoreWithScores(sensorId, from, to);
		}
	}
	
	
	//TODO: Introduce indexed sensors (with some field that is indexed in separate ordered set)
	//TODO: Introduce geo-location indexes (Support in Redis 3.2.X, Geohash as mechanism, check for Java implementations)
	
}
