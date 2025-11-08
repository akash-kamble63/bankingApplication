package com.account_service.service;

import java.time.LocalDateTime;
import java.util.List;

import com.account_service.model.AccountEventStore;

public interface EventSourcingService {
	public void storeEvent(String aggregateId, String eventType, Object eventData, 
            Long userId, String correlationId, String causationId);
	public List<AccountEventStore> getAccountEvents(String accountNumber);
	public List<AccountEventStore> getEventsFromVersion(String accountNumber, Long fromVersion);
	public List<AccountEventStore> getEventsByDateRange(String accountNumber, 
            LocalDateTime start, LocalDateTime end);
	public <T> T rebuildAggregate(String accountNumber, Class<T> aggregateClass);
	public <T> T getSnapshotAtTime(String accountNumber, LocalDateTime pointInTime, 
            Class<T> aggregateClass);
	public long getEventCount(String accountNumber);
	public List<AccountEventStore> getEventsByCorrelation(String correlationId);
}
