package com.account_service.service;

import java.util.Optional;

import com.account_service.model.OutboxEvent;

public interface OutboxService {
	public void saveEvent(String aggregateType, String aggregateId,
			String eventType, String topic, Object payload);

	public void markAsPublished(Long eventId);

	public void markAsFailed(Long eventId, String error);

	public int cleanupOldEvents(int retentionDays);

	public Optional<OutboxEvent> getEvent(Long eventId);

	public void retryEvent(Long eventId);

	public OutboxHealthMetrics getHealthMetrics();

	interface OutboxHealthMetrics {
		long pendingCount();

		long publishedCount();

		long failedCount();

		long permanentlyFailedCount();
	}
}
