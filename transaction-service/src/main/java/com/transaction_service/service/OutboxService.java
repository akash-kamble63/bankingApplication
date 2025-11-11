package com.transaction_service.service;

public interface OutboxService {
	public void saveEvent(String aggregateType, String aggregateId, 
            String eventType, String topic, Object payload);
	public void markAsPublished(Long eventId);
	public void markAsFailed(Long eventId, String error);
	public int cleanupOldEvents(int retentionDays);
}
