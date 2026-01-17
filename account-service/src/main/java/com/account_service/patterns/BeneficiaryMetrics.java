package com.account_service.patterns;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BeneficiaryMetrics {
    private final MeterRegistry meterRegistry;

    /**
     * Record beneficiary creation
     */
    public void recordBeneficiaryCreation(boolean success) {
        Counter.builder("beneficiary.creations")
                .tag("success", String.valueOf(success))
                .description("Number of beneficiary creation attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record beneficiary verification
     */
    public void recordBeneficiaryVerification(boolean success) {
        Counter.builder("beneficiary.verifications")
                .tag("success", String.valueOf(success))
                .description("Number of beneficiary verification attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record beneficiary deletion
     */
    public void recordBeneficiaryDeletion(boolean success) {
        Counter.builder("beneficiary.deletions")
                .tag("success", String.valueOf(success))
                .description("Number of beneficiary deletion attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record duplicate beneficiary attempt
     */
    public void recordDuplicateBeneficiaryAttempt() {
        Counter.builder("beneficiary.duplicate.attempts")
                .description("Number of duplicate beneficiary creation attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record beneficiary limit exceeded
     */
    public void recordBeneficiaryLimitExceeded() {
        Counter.builder("beneficiary.limit.exceeded")
                .description("Number of times beneficiary limit was exceeded")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record beneficiary search operation
     */
    public void recordBeneficiarySearch(int resultCount) {
        Counter.builder("beneficiary.searches")
                .tag("has_results", String.valueOf(resultCount > 0))
                .description("Number of beneficiary search operations")
                .register(meterRegistry)
                .increment();

        Gauge.builder("beneficiary.search.result.count", () -> resultCount)
                .description("Number of results in last search")
                .register(meterRegistry);
    }

    /**
     * Record cache operations
     */
    public void recordCacheOperation(String operation, boolean hit) {
        Counter.builder("beneficiary.cache.operations")
                .tag("operation", operation)
                .tag("result", hit ? "hit" : "miss")
                .description("Beneficiary cache operation statistics")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record idempotency check
     */
    public void recordIdempotencyCheck(boolean duplicate) {
        Counter.builder("beneficiary.idempotency.checks")
                .tag("duplicate", String.valueOf(duplicate))
                .description("Number of idempotency checks")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record event publishing
     */
    public void recordEventPublished(String eventType, boolean success) {
        Counter.builder("beneficiary.events.published")
                .tag("event_type", eventType)
                .tag("success", String.valueOf(success))
                .description("Number of beneficiary events published")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Start timer for beneficiary operations
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record operation duration
     */
    public void recordOperationDuration(Timer.Sample sample, String operation, boolean success) {
        sample.stop(Timer.builder("beneficiary.operation.duration")
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .description("Duration of beneficiary operations")
                .register(meterRegistry));
    }

    /**
     * Record validation failures
     */
    public void recordValidationFailure(String validationType) {
        Counter.builder("beneficiary.validation.failures")
                .tag("type", validationType)
                .description("Number of validation failures")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record Redis failures
     */
    public void recordRedisFailure(String operation) {
        Counter.builder("beneficiary.redis.failures")
                .tag("operation", operation)
                .description("Number of Redis operation failures")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record database query performance
     */
    public void recordDatabaseQuery(Timer.Sample sample, String queryType) {
        sample.stop(Timer.builder("beneficiary.database.query.duration")
                .tag("query_type", queryType)
                .description("Duration of database queries")
                .register(meterRegistry));
    }
}
