package com.account_service.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.model.AccountEventStore;
import com.account_service.repository.EventStoreRepository;
import com.account_service.service.EventSourcingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSourcingServiceImpl implements EventSourcingService {

	private final EventStoreRepository eventStoreRepository;
	private final ObjectMapper objectMapper;

	/**
	 * Store event in event store Must run in existing transaction (MANDATORY)
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	@Retry(name = "eventStoreVersion")
	public void storeEvent(String aggregateId, String eventType, Object eventData, Long userId, String correlationId,
			String causationId) {
		try {
			// Get next version
			Long version = getNextVersion(aggregateId);

			// Serialize event data
			String eventDataJson = objectMapper.writeValueAsString(eventData);

			// Create metadata
			Map<String, Object> metadata = Map.of("userId", userId != null ? userId : 0L, "timestamp",
					LocalDateTime.now(), "source", "account-service");
			String metadataJson = objectMapper.writeValueAsString(metadata);

			// Store event
			AccountEventStore event = AccountEventStore.builder().eventId(UUID.randomUUID().toString())
					.aggregateId(aggregateId).aggregateType("ACCOUNT").eventType(eventType).version(version)
					.eventData(eventDataJson).metadata(metadataJson).userId(userId)
					.correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
					.causationId(causationId).build();

			eventStoreRepository.save(event);
			log.debug("Event stored: aggregateId={}, type={}, version={}", aggregateId, eventType, version);

		} catch (Exception e) {
			log.error("Failed to store event: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to store event", e);
		}
	}

	/**
	 * Get all events for an account (for replay)
	 */
	@Transactional(readOnly = true)
	public List<AccountEventStore> getAccountEvents(String accountNumber) {
		return eventStoreRepository.findByAggregateIdOrderByVersionAsc(accountNumber);
	}

	/**
	 * Get events from specific version onwards
	 */
	@Transactional(readOnly = true)
	public List<AccountEventStore> getEventsFromVersion(String accountNumber, Long fromVersion) {
		return eventStoreRepository.findByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(accountNumber,
				fromVersion);
	}

	/**
	 * Get events within date range
	 */
	@Transactional(readOnly = true)
	public List<AccountEventStore> getEventsByDateRange(String accountNumber, LocalDateTime start, LocalDateTime end) {
		return eventStoreRepository.findByAggregateIdAndTimestampBetweenOrderByVersionAsc(accountNumber, start, end);
	}

	/**
	 * Rebuild account state from events (Event Replay)
	 */
	@Transactional(readOnly = true)
	public <T> T rebuildAggregate(String accountNumber, Class<T> aggregateClass) {
		try {
			List<AccountEventStore> events = getAccountEvents(accountNumber);

			if (events.isEmpty()) {
				return null;
			}

			// Apply events sequentially to rebuild state
			T aggregate = null;
			for (AccountEventStore event : events) {
				if (aggregate == null) {
					// Create initial state from first event
					aggregate = objectMapper.readValue(event.getEventData(), aggregateClass);
				} else {
					// Apply subsequent events (implement applyEvent method in aggregate)
					// This would call aggregate.applyEvent(event)
				}
			}

			log.info("Rebuilt aggregate: accountNumber={}, totalEvents={}", accountNumber, events.size());
			return aggregate;

		} catch (Exception e) {
			log.error("Failed to rebuild aggregate: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to rebuild aggregate", e);
		}
	}

	/**
	 * Get account snapshot at specific point in time
	 */
	@Transactional(readOnly = true)
	public <T> T getSnapshotAtTime(String accountNumber, LocalDateTime pointInTime, Class<T> aggregateClass) {
		try {
			List<AccountEventStore> events = eventStoreRepository.findByAggregateIdAndTimestampBetweenOrderByVersionAsc(
					accountNumber, LocalDateTime.MIN, pointInTime);

			if (events.isEmpty()) {
				return null;
			}

			// Replay events up to point in time
			T aggregate = null;
			for (AccountEventStore event : events) {
				if (aggregate == null) {
					aggregate = objectMapper.readValue(event.getEventData(), aggregateClass);
				}
				// Apply event to aggregate
			}

			log.info("Created snapshot at {}: accountNumber={}", pointInTime, accountNumber);
			return aggregate;

		} catch (Exception e) {
			log.error("Failed to create snapshot: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Get next version number for aggregate
	 */
	private Long getNextVersion(String aggregateId) {
		Optional<Long> latestVersion = eventStoreRepository.findLatestVersion(aggregateId);
		return latestVersion.map(v -> v + 1).orElse(1L);
	}

	/**
	 * Get event count for account
	 */
	@Transactional(readOnly = true)
	public long getEventCount(String accountNumber) {
		return eventStoreRepository.countByAggregateId(accountNumber);
	}

	/**
	 * Get events by correlation ID (for saga tracking)
	 */
	@Transactional(readOnly = true)
	public List<AccountEventStore> getEventsByCorrelation(String correlationId) {
		return eventStoreRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
	}

}
