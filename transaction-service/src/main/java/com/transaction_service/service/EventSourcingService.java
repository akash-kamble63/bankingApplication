package com.transaction_service.service;

public interface EventSourcingService {
	public void storeEvent(String aggregateId, String eventType, Object eventData, 
            Long userId, String correlationId, String causationId);
	
}
