package com.payment_service.service;

public interface OutboxService {
	void saveEvent(String aggregateType, String aggregateId, String eventType,
            String topic, Object payload);
	
}
