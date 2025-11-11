package com.transaction_service.client;

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

	@CircuitBreaker(name = "accountService", fallbackMethod = "placeHoldFallback")
	@Retry(name = "accountService")
	@TimeLimiter(name = "accountService")
	public HoldResponse placeHold(Long accountId, BigDecimal amount, String reason, String txnRef) {
		log.debug("Placing hold on account: {}", accountId);

		return webClient.post().uri("/api/v1/accounts/{accountId}/hold", accountId)
				.header("Idempotency-Key", txnRef + "-HOLD")
				.bodyValue(
						Map.of("amount", amount, "reason", reason, "transactionReference", txnRef, "expiryHours", 24))
				.retrieve().bodyToMono(HoldResponse.class).block(Duration.ofSeconds(5));
	}

	@CircuitBreaker(name = "accountService", fallbackMethod = "debitFallback")
	@Retry(name = "accountService")
	public void debitWithIdempotency(Long accountId, BigDecimal amount, String idempotencyKey, String txnRef) {
		log.debug("Debiting account: {}", accountId);

		webClient.post().uri("/api/v1/accounts/{accountId}/debit", accountId).header("Idempotency-Key", idempotencyKey)
				.bodyValue(Map.of("amount", amount, "reason", "Transfer: " + txnRef, "transactionRef", txnRef))
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(5));
	}

	@CircuitBreaker(name = "accountService", fallbackMethod = "creditFallback")
	@Retry(name = "accountService")
	public void creditWithIdempotency(Long accountId, BigDecimal amount, String idempotencyKey, String txnRef) {
		log.debug("Crediting account: {}", accountId);

		webClient.post().uri("/api/v1/accounts/{accountId}/credit", accountId).header("Idempotency-Key", idempotencyKey)
				.bodyValue(Map.of("amount", amount, "reason", "Transfer: " + txnRef, "transactionRef", txnRef))
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(5));
	}

	@CircuitBreaker(name = "accountService")
	@Retry(name = "accountService")
	public void releaseHold(String holdReference) {
		log.debug("Releasing hold: {}", holdReference);

		webClient.delete().uri("/api/v1/accounts/hold/{holdReference}", holdReference).retrieve().bodyToMono(Void.class)
				.block(Duration.ofSeconds(5));
	}

	// Fallback methods
	private HoldResponse placeHoldFallback(Long accountId, BigDecimal amount, String reason, String txnRef,
			Exception e) {
		log.error("Circuit breaker: placeHold failed - {}", e.getMessage());
		throw new ServiceUnavailableException("Account service unavailable");
	}

	private void debitFallback(Long accountId, BigDecimal amount, String idempotencyKey, String txnRef, Exception e) {
		log.error("Circuit breaker: debit failed - {}", e.getMessage());
		throw new ServiceUnavailableException("Account service unavailable");
	}

	private void creditFallback(Long accountId, BigDecimal amount, String idempotencyKey, String txnRef, Exception e) {
		log.error("Circuit breaker: credit failed - {}", e.getMessage());
		throw new ServiceUnavailableException("Account service unavailable");
	}
}
