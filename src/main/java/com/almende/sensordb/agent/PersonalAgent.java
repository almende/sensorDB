/*
 * Copyright: Almende B.V. (2016), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.sensordb.agent;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.util.TypeUtil;
import com.almende.util.URIUtil;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class PersonalAgent.
 */
@Access(AccessType.PUBLIC)
public class PersonalAgent extends SensorAgent {
	private static final Logger	LOG	= Logger.getLogger(PersonalAgent.class
											.getName());

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

	// Get group invites
	/**
	 * Gets the groups.
	 *
	 * @return the groups
	 */
	public List<String> getGroupInvites() {
		List<String> groupInvites = getState().get("groupInvites",
				new TypeUtil<ArrayList<String>>() {});
		if (groupInvites == null) {
			groupInvites = new ArrayList<String>();
		}
		return groupInvites;
	}

	/**
	 * Adds the membership.
	 *
	 * @param group
	 *            the group
	 */
	public void invite(@Name("group") URI group) {
		final List<String> groupInvites = getGroups();
		if (!groupInvites.contains(group.toASCIIString())) {
			groupInvites.add(group.toASCIIString());
			getState().put("groupInvites", groupInvites);
		}
	}

	/**
	 * Drop invite.
	 *
	 * @param group
	 *            the group
	 */
	public void dropInvite(@Name("group") URI group) {
		final List<String> groupInvites = getGroups();
		if (groupInvites.contains(group.toASCIIString())) {
			groupInvites.remove(group.toASCIIString());
			getState().put("groupInvites", groupInvites);
		}
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

	/**
	 * Gets the graph json.
	 *
	 * @param sender
	 *            the sender
	 * @return the graph json
	 */
	public ObjectNode getGraphData(@Sender URI sender) {
		final ObjectNode result = super.getGraphData(sender);
		for (String group : getGroups()) {
			final URI grp = URIUtil.create(group);
			ObjectNode res;
			try {
				res = getCaller().callSync(grp, "getGraphData", null,
						ObjectNode.class);
				if (res.size() > 0) {
					Iterator<JsonNode> iter = result.elements();
					while (iter.hasNext()) {
						ObjectNode item = (ObjectNode) iter.next();
						String sensor = item.get("label").asText();
						if (res.has(sensor)) {
							if (!item.has("groups")) {
								item.set("groups", JOM.createArrayNode());
							}
							final ObjectNode grpItem = JOM.createObjectNode();
							grpItem.put(
									"name",
									res.get(item.get("label").asText())
											.get("name").asText());
							grpItem.set(
									"values",
									res.get(item.get("label").asText()).get(
											"values"));
							((ArrayNode) item.get("groups")).add(grpItem);
						}
					}
				}
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Failed to get SensorValues", e);
			}
		}

		return result;
	}

	// Data sharing rules (per sensor, for timerange , per target type(group
	// selectors, group aggregator, group members, individual peers))
	// Profile sharing rules (per target type, for timerange)

}
