/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.agent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class PersonalAgent.
 */
@Access(AccessType.PUBLIC)
public class PersonalAgent extends SensorAgent {

	/**
	 * Adds the membership.
	 *
	 * @param group
	 *            the group
	 */
	public void addMembership(@Name("group") URI group) {
		final List<String> groupIds = getGroups();
		if (!groupIds.contains(group.toASCIIString())) {
			groupIds.add(group.toASCIIString());
			getState().put("groupIds", groupIds);
		}
	}

	/**
	 * Gets the groups.
	 *
	 * @return the groups
	 */
	public List<String> getGroups() {
		List<String> groupIds = getState().get("groupIds",
				new TypeUtil<ArrayList<String>>() {});
		if (groupIds == null) {
			groupIds = new ArrayList<String>();
		}
		return groupIds;
	}

	// TODO: implements Personal profile
	/**
	 * Sets the profile field.
	 *
	 * @param fieldname
	 *            the fieldname
	 * @param value
	 *            the value
	 */
	public void setProfileField(String fieldname, String value) {

		final ObjectNode oldProfile = getState().get("profile",
				ObjectNode.class);
		final ObjectNode newProfile = oldProfile.deepCopy();
		if (!getState().putIfUnchanged("profile", newProfile, oldProfile)) {
			setProfileField(fieldname, value);
		}
	}

	/**
	 * Gets the profile field if allowed!.
	 *
	 * @param fieldname
	 *            the fieldname
	 * @param sender
	 *            the sender
	 * @return the profile field
	 */
	public String getProfileField(String fieldname, @Sender URI sender) {
		final ObjectNode profile = getState().get("profile", ObjectNode.class);
		if (profile != null) {
			final JsonNode val = profile.get(fieldname);
			if (val != null) {
				return val.asText();
			}
		}
		return null;
	}

	// Group memberships (Pointer)
	// Data sharing rules (per sensor, for timerange , per target type(group
	// selectors, group aggregator, group members, individual peers))
	// Profile sharing rules (per target type, for timerange)

}
