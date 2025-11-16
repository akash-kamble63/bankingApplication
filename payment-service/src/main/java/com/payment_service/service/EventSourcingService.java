package com.payment_service.service;

import java.util.List;

import com.payment_service.entity.PaymentEvent;

public interface EventSourcingService {
	public void storeEvent(String aggregateId, String eventType, Object eventData, Long userId, String correlationId,
			String causationId);

	public List<PaymentEvent> getEventStream(String aggregateId);

	public Object replayEvents(String aggregateId);
}
