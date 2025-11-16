package com.payment_service.clients;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.exception.PaymentGatewayException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeGatewayClient {
	private final WebClient webClient;

	@CircuitBreaker(name = "stripe", fallbackMethod = "authorizeFallback")
	@Retry(name = "stripe")
	public GatewayAuthorizationResponse authorize(String cardToken, BigDecimal amount, String currency,
			String reference) {
		log.debug("Authorizing payment with Stripe: {}", reference);

		try {
			Map<String, Object> request = Map.of("source", cardToken, "amount",
					amount.multiply(new BigDecimal("100")).longValue(), "currency", currency.toLowerCase(), "capture",
					false, "description", reference);

			Map<String, Object> response = webClient.post().uri("https://api.stripe.com/v1/charges").bodyValue(request)
					.retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
					}).block(Duration.ofSeconds(10));

			return GatewayAuthorizationResponse.builder().success(true).gatewayPaymentId((String) response.get("id"))
					.authorizationCode((String) response.get("authorization_code")).build();

		} catch (Exception e) {
			log.error("Stripe authorization failed: {}", e.getMessage());
			return authorizeFallback(cardToken, amount, currency, reference, e);
		}
	}

	@CircuitBreaker(name = "stripe")
	public GatewayCaptureResponse capture(String paymentId, BigDecimal amount) {
		log.debug("Capturing payment with Stripe: {}", paymentId);

		try {
			Map<String, Object> response = webClient.post()
					.uri("https://api.stripe.com/v1/charges/{id}/capture", paymentId)
					.bodyValue(Map.of("amount", amount.multiply(new BigDecimal("100")).longValue())).retrieve()
					.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
					}).block(Duration.ofSeconds(10));

			return GatewayCaptureResponse.builder().success(true).transactionId((String) response.get("id")).build();

		} catch (Exception e) {
			log.error("Stripe capture failed: {}", e.getMessage());
			throw new PaymentGatewayException("Capture failed", e);
		}
	}

	public void refund(String paymentId, BigDecimal amount) {
		log.debug("Refunding Stripe payment: {}", paymentId);
		// Implement refund logic
	}

	public void voidAuth(String paymentId) {
		log.debug("Voiding Stripe authorization: {}", paymentId);
		// Implement void logic
	}

	private GatewayAuthorizationResponse authorizeFallback(String cardToken, BigDecimal amount, String currency,
			String reference, Exception e) {
		log.error("Stripe fallback triggered: {}", e.getMessage());
		return GatewayAuthorizationResponse.builder().success(false)
				.errorMessage("Stripe gateway temporarily unavailable").errorCode("GATEWAY_ERROR").build();
	}
}
