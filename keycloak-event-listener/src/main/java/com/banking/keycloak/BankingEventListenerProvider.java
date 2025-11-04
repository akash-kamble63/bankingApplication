package com.banking.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BankingEventListenerProvider implements EventListenerProvider {
	private static final Logger log = Logger.getLogger(BankingEventListenerProvider.class);
	private final String webhookUrl;
	private final String webhookSecret;
	private final WebhookSender webhookSender;
	private final ObjectMapper objectMapper;

	public BankingEventListenerProvider(String webhookUrl, String webhookSecret) {
		this.webhookUrl = webhookUrl;
		this.webhookSecret = webhookSecret;
		this.webhookSender = new WebhookSender();
		this.objectMapper = new ObjectMapper();
	}
	
	/**
	 * convert Keycloak event to JSON and send it to webhook
	 * @param Event
	 */
	@Override
	public void onEvent(Event event) {
		log.infof("User Event: type=%s, realmId=%s, userId=%s", event.getType(), event.getRealmId(), event.getUserId());
		if (shouldHandleEvent(event.getType())) {
			try {
				ObjectNode payload = buildEventPayload(event);
				webhookSender.sendWebhook(webhookUrl, webhookSecret, payload.toString());
				log.infof("Webhook sent successfully for event: %s", event.getType());
			} catch (Exception e) {
				log.error("Failed to send webhook", e);
			}
		}
	}

	/**
	 * Triggered automatically by Keycloak whenever an admin action occurs
	 * @param AdminEvent
	 * @param includeRepresentation A flag that indicates whether the JSON representation of the resource should be included in the event
	 */

	@Override
	public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
		log.infof("Admin Event: operationType=%s, resourceType=%s, resourcePath=%s", adminEvent.getOperationType(),
				adminEvent.getResourceType(), adminEvent.getResourcePath());

		// Handle admin events (user updates, deletions, etc.)
		if (shouldHandleAdminEvent(adminEvent)) {
			try {
				ObjectNode payload = buildAdminEventPayload(adminEvent);
				webhookSender.sendWebhook(webhookUrl, webhookSecret, payload.toString());
				log.infof("Webhook sent successfully for admin event: %s", adminEvent.getOperationType());
			} catch (Exception e) {
				log.error("Failed to send webhook for admin event", e);
			}
		}

	}

	/**
	 * Determine which user events to handle
	 */
	private boolean shouldHandleEvent(EventType eventType) {
		return eventType == EventType.VERIFY_EMAIL || eventType == EventType.UPDATE_EMAIL
				|| eventType == EventType.UPDATE_PASSWORD || eventType == EventType.DELETE_ACCOUNT
				|| eventType == EventType.REGISTER || eventType == EventType.LOGIN || eventType == EventType.LOGOUT;
	}

	/**
	 * Determine which admin events to handle
	 */
	private boolean shouldHandleAdminEvent(AdminEvent adminEvent) {
		ResourceType resourceType = adminEvent.getResourceType();
		OperationType operationType = adminEvent.getOperationType();

		// User updates, deletions, creations
		if (resourceType == ResourceType.USER) {
			return operationType == OperationType.UPDATE || operationType == OperationType.DELETE
					|| operationType == OperationType.CREATE;
		}

		return false;
	}

	/**
	 * Build payload for user events
	 */
	private ObjectNode buildEventPayload(Event event) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("eventType", "USER_EVENT");
		payload.put("type", event.getType().toString());
		payload.put("realmId", event.getRealmId());
		payload.put("clientId", event.getClientId());
		payload.put("userId", event.getUserId());
		payload.put("ipAddress", event.getIpAddress());
		payload.put("time", event.getTime());

		if (event.getError() != null) {
			payload.put("error", event.getError());
		}

		if (event.getDetails() != null) {
			ObjectNode details = objectMapper.createObjectNode();
			event.getDetails().forEach(details::put);
			payload.set("details", details);
		}

		return payload;
	}

	/**
	 * Build payload for admin events
	 */
	private ObjectNode buildAdminEventPayload(AdminEvent adminEvent) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("eventType", "ADMIN_EVENT");
		payload.put("operationType", adminEvent.getOperationType().toString());
		payload.put("resourceType", adminEvent.getResourceType().toString());
		payload.put("resourcePath", adminEvent.getResourcePath());
		payload.put("realmId", adminEvent.getRealmId());
		payload.put("time", adminEvent.getTime());

		if (adminEvent.getError() != null) {
			payload.put("error", adminEvent.getError());
		}

		// Extract userId from resourcePath if it's a user operation
		if (adminEvent.getResourceType() == ResourceType.USER) {
			String resourcePath = adminEvent.getResourcePath();
			String userId = extractUserIdFromPath(resourcePath);
			if (userId != null) {
				payload.put("userId", userId);
			}
		}

		return payload;
	}

	/**
	 * Extract user ID from resource path
	 */
	private String extractUserIdFromPath(String resourcePath) {
		if (resourcePath != null && resourcePath.startsWith("users/")) {
			String[] parts = resourcePath.split("/");
			if (parts.length > 1) {
				return parts[1];
			}
		}
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}
}
