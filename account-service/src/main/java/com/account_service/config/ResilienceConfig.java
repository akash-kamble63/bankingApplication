package com.account_service.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ResilienceConfig {
    /**
     * Circuit Breaker for external service calls (e.g., verification services)
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .slowCallRateThreshold(50) // Open circuit if 50% of calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // Call is slow if > 2s
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
                .permittedNumberOfCallsInHalfOpenState(5) // Allow 5 calls in half-open
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10) // Track last 10 calls
                .minimumNumberOfCalls(5) // Need 5 calls before calculating failure rate
                .recordExceptions(Exception.class) // Record all exceptions
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Register event listeners
        registry.circuitBreaker("beneficiaryService")
                .getEventPublisher()
                .onStateTransition(event -> log.warn("Circuit Breaker state changed: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.error("Circuit Breaker recorded error: {}", event.getThrowable().getMessage()));

        return registry;
    }

    /**
     * Circuit Breaker for Kafka operations
     */
    @Bean
    public CircuitBreaker kafkaCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig kafkaConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(60) // More tolerant for messaging
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();

        return registry.circuitBreaker("kafkaPublisher", kafkaConfig);
    }

    /**
     * Circuit Breaker for Redis operations
     */
    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig redisConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(70) // Very tolerant - Redis is optional
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slidingWindowSize(15)
                .minimumNumberOfCalls(5)
                .build();

        return registry.circuitBreaker("redisCache", redisConfig);
    }

    /**
     * Rate Limiter Registry
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 requests
                .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
                .timeoutDuration(Duration.ofSeconds(5)) // wait max 5s for permission
                .build();

        return RateLimiterRegistry.of(config);
    }

    /**
     * Rate Limiter for beneficiary operations
     */
    @Bean
    public RateLimiter beneficiaryRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50) // 50 beneficiary operations
                .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute per user
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        RateLimiter rateLimiter = registry.rateLimiter("beneficiaryOperations", config);

        rateLimiter.getEventPublisher()
                .onSuccess(event -> log.debug("Rate limiter acquired permission"))
                .onFailure(event -> log.warn("Rate limiter rejected request - limit exceeded"));

        return rateLimiter;
    }

    /**
     * Rate Limiter for search operations (more restrictive)
     */
    @Bean
    public RateLimiter searchRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(30) // 30 searches
                .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
                .timeoutDuration(Duration.ofSeconds(2))
                .build();

        return registry.rateLimiter("beneficiarySearch", config);
    }

    /**
     * Retry configuration for transient failures
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);

        registry.retry("beneficiaryService")
                .getEventPublisher()
                .onRetry(event -> log.warn("Retry attempt {} for operation", event.getNumberOfRetryAttempts()));

        return registry;
    }

    /**
     * Retry for database operations
     */
    @Bean
    public Retry databaseRetry(RetryRegistry registry) {
        RetryConfig dbConfig = RetryConfig.custom()
                .maxAttempts(2) // Quick retry for DB
                .waitDuration(Duration.ofMillis(100))
                .build();

        return registry.retry("databaseOperations", dbConfig);
    }
}
