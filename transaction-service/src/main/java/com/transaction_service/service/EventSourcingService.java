package com.transaction_service.service;

import java.util.List;

import com.transaction_service.entity.TransactionEventStore;

public interface EventSourcingService {
	void storeEvent(String aggregateId, String eventType, Object eventData, 
            Long userId, String correlationId, String causationId);
List<TransactionEventStore> getTransactionEvents(String transactionReference);
List<TransactionEventStore> getEventsFromVersion(String transactionReference, Long fromVersion);
}
