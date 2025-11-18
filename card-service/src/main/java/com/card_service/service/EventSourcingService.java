package com.card_service.service;

public interface EventSourcingService {
	void storeEvent(String aggregateId, String eventType, Object eventData, Long userId, String correlationId,
			String causationId);
}
