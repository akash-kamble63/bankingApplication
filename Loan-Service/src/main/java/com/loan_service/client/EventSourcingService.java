package com.loan_service.client;

import org.springframework.stereotype.Service;

@Service
public class EventSourcingService {
	public void storeEvent(String aggregateId, String eventType, Object eventData,
            Long userId, String correlationId, Object causationId) {
}
}
