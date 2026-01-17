package com.payment_service.clients;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.exception.InvalidRequestException;
import com.payment_service.exception.PaymentGatewayException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for interacting with Stripe payment gateway.
 * Implements authorization, capture, refund, and void operations with
 * resilience patterns.
 */
@Slf4j
@Service
public class StripeGatewayClient {

	private static final String GATEWAY_NAME = "stripe";
	private static final String SUCCEEDED_STATUS = "succeeded";
	private static final String PENDING_STATUS = "pending";
	private static final BigDecimal CENTS_MULTIPLIER = new BigDecimal("100");
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF = new ParameterizedTypeReference<Map<String, Object>>() {
	};

	private final WebClient webClient;
	private final String baseUrl;

	public StripeGatewayClient(
			WebClient.Builder webClientBuilder,
			@Value("${payment.gateway.stripe.base-url:https://api.stripe.com/v1}") String baseUrl,
			@Value("${payment.gateway.stripe.secret-key}") String secretKey) {

		this.baseUrl = baseUrl;
		this.webClient = webClientBuilder
				.baseUrl(baseUrl)
				.defaultHeader("Authorization", "Bearer " + secretKey)
				.defaultHeader("Content-Type", "application/x-www-form-urlencoded")
				.build();

		log.info("StripeGatewayClient initialized with base URL: {}", baseUrl);
	}

	/**
	 * Authorizes a payment without capturing funds.
	 *
	 * @param cardToken the tokenized card information (e.g., tok_xxx or source ID)
	 * @param amount    the amount to authorize
	 * @param currency  the currency code (e.g., USD, EUR)
	 * @param reference unique reference for this transaction
	 * @return authorization response with gateway payment ID
	 * @throws InvalidRequestException if input validation fails
	 */
	@CircuitBreaker(name = GATEWAY_NAME, fallbackMethod = "authorizeFallback")
	@Retry(name = GATEWAY_NAME)
	public GatewayAuthorizationResponse authorize(
			String cardToken,
			BigDecimal amount,
			String currency,
			String reference) {

		validateAuthorizeRequest(cardToken, amount, currency, reference);

		log.info("Authorizing payment with Stripe - Reference: {}, Amount: {} {}",
				reference, amount, currency);

		try {
			MultiValueMap<String, String> formData = buildAuthFormData(
					cardToken, amount, currency, reference);

			Map<String, Object> response = webClient.post()
					.uri("/charges")
					.body(BodyInserters.fromFormData(formData))
					.retrieve()
					.bodyToMono(MAP_TYPE_REF)
					.timeout(REQUEST_TIMEOUT)
					.block();

			return mapAuthResponse(response);

		} catch (WebClientResponseException e) {
			log.error("Stripe authorization failed - Reference: {}, Status: {}, Body: {}",
					reference, e.getStatusCode(), e.getResponseBodyAsString());
			throw new PaymentGatewayException(
					String.format("Authorization failed: %s", e.getMessage()),
					e);
		} catch (Exception e) {
			log.error("Unexpected error during Stripe authorization - Reference: {}", reference, e);
			throw new PaymentGatewayException("Unexpected authorization error", e);
		}
	}

	/**
	 * Captures a previously authorized payment.
	 *
	 * @param paymentId the Stripe charge ID from authorization
	 * @param amount    the amount to capture (must not exceed authorized amount)
	 * @return capture response with transaction ID
	 * @throws PaymentGatewayException if capture fails
	 */
	@CircuitBreaker(name = GATEWAY_NAME, fallbackMethod = "captureFallback")
	@Retry(name = GATEWAY_NAME)
	public GatewayCaptureResponse capture(String paymentId, BigDecimal amount) {

		validateCaptureRequest(paymentId, amount);

		log.info("Capturing payment with Stripe - ChargeId: {}, Amount: {}", paymentId, amount);

		try {
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("amount", String.valueOf(convertToCents(amount)));

			Map<String, Object> response = webClient.post()
					.uri("/charges/{id}/capture", paymentId)
					.body(BodyInserters.fromFormData(formData))
					.retrieve()
					.bodyToMono(MAP_TYPE_REF)
					.timeout(REQUEST_TIMEOUT)
					.block();

			return mapCaptureResponse(response);

		} catch (WebClientResponseException e) {
			log.error("Stripe capture failed - ChargeId: {}, Status: {}, Body: {}",
					paymentId, e.getStatusCode(), e.getResponseBodyAsString());
			throw new PaymentGatewayException(
					String.format("Capture failed for charge %s: %s", paymentId, e.getMessage()),
					e);
		} catch (Exception e) {
			log.error("Unexpected error during Stripe capture - ChargeId: {}", paymentId, e);
			throw new PaymentGatewayException("Unexpected capture error", e);
		}
	}

	/**
	 * Refunds a captured payment.
	 *
	 * @param paymentId the Stripe charge ID
	 * @param amount    the amount to refund (null for full refund)
	 * @return refund transaction ID
	 * @throws PaymentGatewayException if refund fails
	 */
	@CircuitBreaker(name = GATEWAY_NAME)
	@Retry(name = GATEWAY_NAME)
	public String refund(String paymentId, BigDecimal amount) {

		validateRefundRequest(paymentId);

		log.info("Refunding Stripe payment - ChargeId: {}, Amount: {}",
				paymentId, amount != null ? amount : "FULL");

		try {
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("charge", paymentId);

			if (amount != null) {
				formData.add("amount", String.valueOf(convertToCents(amount)));
			}

			Map<String, Object> response = webClient.post()
					.uri("/refunds")
					.body(BodyInserters.fromFormData(formData))
					.retrieve()
					.bodyToMono(MAP_TYPE_REF)
					.timeout(REQUEST_TIMEOUT)
					.block();

			String refundId = extractStringValue(response, "id");
			String status = extractStringValue(response, "status");

			if (refundId != null && (SUCCEEDED_STATUS.equals(status) || PENDING_STATUS.equals(status))) {
				log.info("Refund successful - ChargeId: {}, RefundId: {}, Status: {}",
						paymentId, refundId, status);
				return refundId;
			}

			throw new PaymentGatewayException(
					String.format("Refund failed - invalid status: %s", status));

		} catch (WebClientResponseException e) {
			log.error("Stripe refund failed - ChargeId: {}, Status: {}, Body: {}",
					paymentId, e.getStatusCode(), e.getResponseBodyAsString());
			throw new PaymentGatewayException(
					String.format("Refund failed for charge %s: %s", paymentId, e.getMessage()),
					e);
		} catch (Exception e) {
			log.error("Unexpected error during Stripe refund - ChargeId: {}", paymentId, e);
			throw new PaymentGatewayException("Unexpected refund error", e);
		}
	}

	/**
	 * Voids an authorized but not captured payment.
	 * In Stripe, this is done by refunding an uncaptured charge.
	 *
	 * @param paymentId the Stripe charge ID to void
	 * @throws PaymentGatewayException if void fails
	 */
	@CircuitBreaker(name = GATEWAY_NAME)
	@Retry(name = GATEWAY_NAME)
	public void voidAuth(String paymentId) {

		Objects.requireNonNull(paymentId, "Payment ID cannot be null");

		log.info("Voiding Stripe authorization - ChargeId: {}", paymentId);

		try {
			// In Stripe, voiding an uncaptured charge is done by refunding it
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("charge", paymentId);

			Map<String, Object> response = webClient.post()
					.uri("/refunds")
					.body(BodyInserters.fromFormData(formData))
					.retrieve()
					.bodyToMono(MAP_TYPE_REF)
					.timeout(REQUEST_TIMEOUT)
					.block();

			String status = extractStringValue(response, "status");

			if (SUCCEEDED_STATUS.equals(status) || PENDING_STATUS.equals(status)) {
				log.info("Authorization voided successfully - ChargeId: {}", paymentId);
			} else {
				throw new PaymentGatewayException(
						String.format("Void failed - unexpected status: %s", status));
			}

		} catch (WebClientResponseException e) {
			log.error("Stripe void failed - ChargeId: {}, Status: {}, Body: {}",
					paymentId, e.getStatusCode(), e.getResponseBodyAsString());
			throw new PaymentGatewayException(
					String.format("Void failed for charge %s: %s", paymentId, e.getMessage()),
					e);
		} catch (Exception e) {
			log.error("Unexpected error during Stripe void - ChargeId: {}", paymentId, e);
			throw new PaymentGatewayException("Unexpected void error", e);
		}
	}

	// Private helper methods

	private void validateAuthorizeRequest(String cardToken, BigDecimal amount,
			String currency, String reference) {
		Objects.requireNonNull(cardToken, "Card token cannot be null");
		Objects.requireNonNull(amount, "Amount cannot be null");
		Objects.requireNonNull(currency, "Currency cannot be null");
		Objects.requireNonNull(reference, "Reference cannot be null");

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidRequestException("Amount must be greater than zero");
		}

		if (cardToken.trim().isEmpty()) {
			throw new InvalidRequestException("Card token cannot be empty");
		}
	}

	private void validateCaptureRequest(String paymentId, BigDecimal amount) {
		Objects.requireNonNull(paymentId, "Payment ID cannot be null");
		Objects.requireNonNull(amount, "Amount cannot be null");

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidRequestException("Capture amount must be greater than zero");
		}
	}

	private void validateRefundRequest(String paymentId) {
		Objects.requireNonNull(paymentId, "Payment ID cannot be null");
	}

	private MultiValueMap<String, String> buildAuthFormData(
			String cardToken, BigDecimal amount, String currency, String reference) {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("source", cardToken);
		formData.add("amount", String.valueOf(convertToCents(amount)));
		formData.add("currency", currency.toLowerCase());
		formData.add("capture", "false"); // Authorization only
		formData.add("description", reference);
		formData.add("metadata[reference]", reference);

		return formData;
	}

	private long convertToCents(BigDecimal amount) {
		return amount.multiply(CENTS_MULTIPLIER).longValue();
	}

	private GatewayAuthorizationResponse mapAuthResponse(Map<String, Object> response) {
		if (response == null) {
			throw new PaymentGatewayException("Received null response from Stripe");
		}

		String chargeId = extractStringValue(response, "id");
		String status = extractStringValue(response, "status");
		Boolean captured = (Boolean) response.get("captured");
		String authCode = extractStringValue(response, "authorization_code");

		// For authorization, we expect status to be 'succeeded' and captured to be
		// false
		boolean isSuccess = SUCCEEDED_STATUS.equals(status) && Boolean.FALSE.equals(captured);

		Map<String, Object> error = extractErrorInfo(response);

		return GatewayAuthorizationResponse.builder()
				.success(isSuccess)
				.gatewayPaymentId(chargeId)
				.authorizationCode(authCode)
				.errorCode(error != null ? (String) error.get("code") : null)
				.errorMessage(error != null ? (String) error.get("message") : null)
				.build();
	}

	private GatewayCaptureResponse mapCaptureResponse(Map<String, Object> response) {
		if (response == null) {
			throw new PaymentGatewayException("Received null response from Stripe");
		}

		String chargeId = extractStringValue(response, "id");
		String status = extractStringValue(response, "status");
		Boolean captured = (Boolean) response.get("captured");

		boolean isSuccess = SUCCEEDED_STATUS.equals(status) && Boolean.TRUE.equals(captured);

		Map<String, Object> error = extractErrorInfo(response);

		return GatewayCaptureResponse.builder()
				.success(isSuccess)
				.transactionId(chargeId)
				.errorCode(error != null ? (String) error.get("code") : null)
				.errorMessage(error != null ? (String) error.get("message") : null)
				.build();
	}

	private String extractStringValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractErrorInfo(Map<String, Object> response) {
		Object errorObj = response.get("error");
		if (errorObj instanceof Map) {
			return (Map<String, Object>) errorObj;
		}

		Object failureObj = response.get("failure_message");
		if (failureObj != null) {
			Map<String, Object> error = new HashMap<>();
			error.put("message", failureObj.toString());
			error.put("code", response.get("failure_code"));
			return error;
		}

		return null;
	}

	// Fallback methods

	private GatewayAuthorizationResponse authorizeFallback(
			String cardToken,
			BigDecimal amount,
			String currency,
			String reference,
			Exception e) {

		log.error("Stripe authorization fallback triggered - Reference: {}, Error: {}",
				reference, e.getMessage());

		return GatewayAuthorizationResponse.builder()
				.success(false)
				.errorMessage("Stripe gateway temporarily unavailable. Please try again later.")
				.errorCode("GATEWAY_UNAVAILABLE")
				.build();
	}

	private GatewayCaptureResponse captureFallback(
			String paymentId,
			BigDecimal amount,
			Exception e) {

		log.error("Stripe capture fallback triggered - ChargeId: {}, Error: {}",
				paymentId, e.getMessage());

		return GatewayCaptureResponse.builder()
				.success(false)
				.errorMessage("Stripe gateway temporarily unavailable. Please try again later.")
				.errorCode("GATEWAY_UNAVAILABLE")
				.build();
	}
}