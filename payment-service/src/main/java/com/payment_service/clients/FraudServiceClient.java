package com.payment_service.clients;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.payment_service.DTOs.FraudCheckResult;
import com.payment_service.DTOs.PaymentSagaData;
import com.payment_service.enums.PaymentMethod;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudServiceClient {
	private final WebClient fraudServiceWebClient;

	@CircuitBreaker(name = "fraud-service", fallbackMethod = "quickCheckFallback")
	public FraudCheckResult quickCheck(Long userId, BigDecimal amount, PaymentMethod method, String reference) {
		log.debug("Performing quick fraud check for user: {}", userId);

		return fraudServiceWebClient.post().uri("/api/v1/fraud/quick-check")
				.bodyValue(Map.of("userId", userId, "amount", amount, "paymentMethod", method.name(), "reference",
						reference))
				.retrieve().bodyToMono(FraudCheckResult.class).timeout(Duration.ofMillis(50)) // Fast check
				.block();
	}

	public void deepAnalysis(PaymentSagaData data) {
		log.debug("Triggering deep fraud analysis for: {}", data.getPaymentReference());

		fraudServiceWebClient.post().uri("/api/v1/fraud/deep-analysis").bodyValue(data).retrieve()
				.bodyToMono(Void.class).subscribe(); // Async - don't wait
	}

	private FraudCheckResult quickCheckFallback(Long userId, BigDecimal amount, PaymentMethod method, String reference,
			Exception e) {
		log.warn("Fraud check fallback triggered for user: {}, assuming safe", userId);

		// Default to safe with low score when fraud service is down
		return FraudCheckResult.builder().blocked(false).fraudScore(new BigDecimal("10.0")).riskLevel("MEDIUM")
				.reason("Fraud service unavailable - default approval").build();
	}
}
