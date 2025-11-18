package com.card_service.service;

public interface OutboxService {
	void saveEvent(String aggregateType, String aggregateId, String eventType, String topic, Object payload);

	void markAsPublished(Long eventId);

	void markAsFailed(Long eventId, String error);

	int cleanupOldEvents(int retentionDays);
}
