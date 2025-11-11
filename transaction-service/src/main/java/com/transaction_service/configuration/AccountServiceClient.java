package com.transaction_service.configuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.transaction_service.DTOs.HoldResponse;
import com.transaction_service.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceClient {
	private final WebClient webClient;

	/**
	 * Async, Non-blocking, with Circuit Breaker and Retry
	 */
	@CircuitBreaker(name = "accountService", fallbackMethod = "placeHoldFallback")
	@Retry(name = "accountService", fallbackMethod = "placeHoldFallback")
	@TimeLimiter(name = "accountService")
	public HoldResponse placeHold(Long accountId, BigDecimal amount, String reason, String txnRef) {

		return webClient.post().uri("/api/v1/accounts/{accountId}/hold", accountId)
				.header("Idempotency-Key", txnRef + "-HOLD")
				.bodyValue(Map.of("amount", amount, "reason", reason, "transactionReference", txnRef)).retrieve()
				.bodyToMono(HoldResponse.class).block(Duration.ofSeconds(5)); // Timeout
	}

	// Fallback method
	private HoldResponse placeHoldFallback(Long accountId, BigDecimal amount, String reason, String txnRef,
			Exception e) {
		log.error("Circuit breaker fallback for placeHold: {}", e.getMessage());
		throw new ServiceUnavailableException("Account service temporarily unavailable");
	}
}
