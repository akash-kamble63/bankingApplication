package com.account_service.patterns;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountMetrics {
	private final MeterRegistry meterRegistry;

	/**
	 * Record balance update operation
	 */
	public void recordBalanceUpdate(String operation, boolean success) {
		Counter.builder("account.balance.updates").tag("operation", operation).tag("success", String.valueOf(success))
				.description("Number of balance update operations").register(meterRegistry).increment();
	}

	/**
	 * Record concurrency conflict
	 */
	public void recordConcurrencyConflict(String operation) {
		Counter.builder("account.concurrency.conflicts").tag("operation", operation)
				.description("Number of concurrency conflicts detected").register(meterRegistry).increment();
	}

	/**
	 * Record account creation
	 */
	public void recordAccountCreation(String accountType) {
		Counter.builder("account.creations").tag("type", accountType).description("Number of accounts created")
				.register(meterRegistry).increment();
	}

	/**
	 * Record hold placement
	 */
	public void recordHoldPlacement(boolean success) {
		Counter.builder("account.holds.placed").tag("success", String.valueOf(success))
				.description("Number of holds placed").register(meterRegistry).increment();
	}

	/**
	 * Record hold release
	 */
	public void recordHoldRelease(String releaseType) {
		Counter.builder("account.holds.released").tag("type", releaseType) // MANUAL, EXPIRED, AUTO
				.description("Number of holds released").register(meterRegistry).increment();
	}

	/**
	 * Record event store operation timing
	 */
	public Timer.Sample startEventStoreTimer() {
		return Timer.start(meterRegistry);
	}

	public void recordEventStoreOperation(Timer.Sample sample, String eventType, boolean success) {
		sample.stop(Timer.builder("account.event.store.duration").tag("event_type", eventType)
				.tag("success", String.valueOf(success)).description("Duration of event store operations")
				.register(meterRegistry));
	}

	/**
	 * Record outbox processing
	 */
	public void recordOutboxProcessing(int eventCount, boolean success) {
		Counter.builder("account.outbox.processed").tag("success", String.valueOf(success))
				.description("Number of outbox events processed").register(meterRegistry).increment(eventCount);
	}

	/**
	 * Record saga operation
	 */
	public void recordSagaOperation(String sagaType, String status) {
		Counter.builder("account.saga.operations").tag("saga_type", sagaType).tag("status", status)
				.description("Number of saga operations").register(meterRegistry).increment();
	}

	/**
	 * Record cache hit/miss
	 */
	public void recordCacheOperation(String cacheName, boolean hit) {
		Counter.builder("account.cache.operations").tag("cache", cacheName).tag("result", hit ? "hit" : "miss")
				.description("Cache operation statistics").register(meterRegistry).increment();
	}

	public Timer.Sample startTimer() {
		return Timer.start(meterRegistry);
	}
}
