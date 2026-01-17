package com.payment_service.clients;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.DTOs.RazorpayAuthRequest;
import com.payment_service.DTOs.RazorpayAuthResponse;
import com.payment_service.DTOs.RazorpayCaptureResponse;
import com.payment_service.DTOs.RazorpayRefundRequest;
import com.payment_service.DTOs.RazorpayRefundResponse;
import com.payment_service.exception.PaymentGatewayException;
import com.payment_service.exception.InvalidRequestException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for interacting with Razorpay payment gateway.
 * Implements authorization, capture, refund, and void operations with
 * resilience patterns.
 */
@Slf4j
@Service
public class RazorpayGatewayClient {

    private static final String GATEWAY_NAME = "razorpay";
    private static final String AUTHORIZED_STATUS = "authorized";
    private static final String CAPTURED_STATUS = "captured";
    private static final String REFUNDED_STATUS = "refunded";
    private static final BigDecimal PAISE_MULTIPLIER = new BigDecimal("100");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final String baseUrl;

    public RazorpayGatewayClient(
            WebClient.Builder webClientBuilder,
            @Value("${payment.gateway.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl,
            @Value("${payment.gateway.razorpay.api-key}") String apiKey,
            @Value("${payment.gateway.razorpay.api-secret}") String apiSecret) {

        this.baseUrl = baseUrl;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(apiKey, apiSecret))
                .build();

        log.info("RazorpayGatewayClient initialized with base URL: {}", baseUrl);
    }

    /**
     * Authorizes a payment without capturing funds.
     *
     * @param cardToken the tokenized card information
     * @param amount    the amount to authorize
     * @param currency  the currency code (e.g., INR, USD)
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

        log.info("Authorizing payment with Razorpay - Reference: {}, Amount: {} {}",
                reference, amount, currency);

        try {
            RazorpayAuthRequest request = buildAuthRequest(cardToken, amount, currency, reference);

            RazorpayAuthResponse response = webClient.post()
                    .uri("/payments")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RazorpayAuthResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            return mapAuthResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Razorpay authorization failed - Reference: {}, Status: {}, Body: {}",
                    reference, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentGatewayException(
                    String.format("Authorization failed: %s", e.getMessage()),
                    e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay authorization - Reference: {}", reference, e);
            throw new PaymentGatewayException("Unexpected authorization error", e);
        }
    }

    /**
     * Captures a previously authorized payment.
     *
     * @param paymentId the gateway payment ID from authorization
     * @param amount    the amount to capture (must not exceed authorized amount)
     * @return capture response with transaction ID
     * @throws PaymentGatewayException if capture fails
     */
    @CircuitBreaker(name = GATEWAY_NAME, fallbackMethod = "captureFallback")
    @Retry(name = GATEWAY_NAME)
    public GatewayCaptureResponse capture(String paymentId, BigDecimal amount) {

        validateCaptureRequest(paymentId, amount);

        log.info("Capturing payment with Razorpay - PaymentId: {}, Amount: {}", paymentId, amount);

        try {
            long amountInPaise = convertToPaise(amount);

            RazorpayCaptureResponse response = webClient.post()
                    .uri("/payments/{id}/capture", paymentId)
                    .bodyValue(Map.of("amount", amountInPaise))
                    .retrieve()
                    .bodyToMono(RazorpayCaptureResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            return mapCaptureResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Razorpay capture failed - PaymentId: {}, Status: {}, Body: {}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentGatewayException(
                    String.format("Capture failed for payment %s: %s", paymentId, e.getMessage()),
                    e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay capture - PaymentId: {}", paymentId, e);
            throw new PaymentGatewayException("Unexpected capture error", e);
        }
    }

    /**
     * Refunds a captured payment.
     *
     * @param paymentId the gateway payment ID
     * @param amount    the amount to refund (null for full refund)
     * @return refund transaction ID
     * @throws PaymentGatewayException if refund fails
     */
    @CircuitBreaker(name = GATEWAY_NAME)
    @Retry(name = GATEWAY_NAME)
    public String refund(String paymentId, BigDecimal amount) {

        validateRefundRequest(paymentId);

        log.info("Refunding Razorpay payment - PaymentId: {}, Amount: {}",
                paymentId, amount != null ? amount : "FULL");

        try {
            RazorpayRefundRequest.RazorpayRefundRequestBuilder requestBuilder = RazorpayRefundRequest.builder();

            if (amount != null) {
                requestBuilder.amount(convertToPaise(amount));
            }

            RazorpayRefundResponse response = webClient.post()
                    .uri("/payments/{id}/refund", paymentId)
                    .bodyValue(requestBuilder.build())
                    .retrieve()
                    .bodyToMono(RazorpayRefundResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response != null && REFUNDED_STATUS.equals(response.getStatus())) {
                log.info("Refund successful - PaymentId: {}, RefundId: {}",
                        paymentId, response.getId());
                return response.getId();
            }

            throw new PaymentGatewayException("Refund failed - invalid response status");

        } catch (WebClientResponseException e) {
            log.error("Razorpay refund failed - PaymentId: {}, Status: {}, Body: {}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentGatewayException(
                    String.format("Refund failed for payment %s: %s", paymentId, e.getMessage()),
                    e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay refund - PaymentId: {}", paymentId, e);
            throw new PaymentGatewayException("Unexpected refund error", e);
        }
    }

    /**
     * Voids an authorized but not captured payment.
     *
     * @param paymentId the gateway payment ID to void
     * @throws PaymentGatewayException if void fails
     */
    @CircuitBreaker(name = GATEWAY_NAME)
    @Retry(name = GATEWAY_NAME)
    public void voidAuth(String paymentId) {

        Objects.requireNonNull(paymentId, "Payment ID cannot be null");

        log.info("Voiding Razorpay authorization - PaymentId: {}", paymentId);

        try {
            // Razorpay automatically voids uncaptured authorizations after a timeout
            // For explicit void, we can refund with amount 0 or use payments/{id}/void if
            // available
            webClient.post()
                    .uri("/payments/{id}/void", paymentId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            log.info("Authorization voided successfully - PaymentId: {}", paymentId);

        } catch (WebClientResponseException e) {
            log.error("Razorpay void failed - PaymentId: {}, Status: {}, Body: {}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentGatewayException(
                    String.format("Void failed for payment %s: %s", paymentId, e.getMessage()),
                    e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay void - PaymentId: {}", paymentId, e);
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

    private RazorpayAuthRequest buildAuthRequest(String cardToken, BigDecimal amount,
            String currency, String reference) {
        return RazorpayAuthRequest.builder()
                .token(cardToken)
                .amount(convertToPaise(amount))
                .currency(currency.toUpperCase())
                .receipt(reference)
                .capture(false) // Authorization only, not capture
                .build();
    }

    private long convertToPaise(BigDecimal amount) {
        return amount.multiply(PAISE_MULTIPLIER).longValue();
    }

    private GatewayAuthorizationResponse mapAuthResponse(RazorpayAuthResponse response) {
        if (response == null) {
            throw new PaymentGatewayException("Received null response from Razorpay");
        }

        boolean isSuccess = AUTHORIZED_STATUS.equalsIgnoreCase(response.getStatus());

        return GatewayAuthorizationResponse.builder()
                .success(isSuccess)
                .gatewayPaymentId(response.getId())
                .authorizationCode(response.getAuthCode())
                .errorCode(isSuccess ? null : response.getErrorCode())
                .errorMessage(isSuccess ? null : response.getErrorDescription())
                .build();
    }

    private GatewayCaptureResponse mapCaptureResponse(RazorpayCaptureResponse response) {
        if (response == null) {
            throw new PaymentGatewayException("Received null response from Razorpay");
        }

        boolean isSuccess = CAPTURED_STATUS.equalsIgnoreCase(response.getStatus());

        return GatewayCaptureResponse.builder()
                .success(isSuccess)
                .transactionId(response.getId())
                .errorCode(isSuccess ? null : response.getErrorCode())
                .errorMessage(isSuccess ? null : response.getErrorDescription())
                .build();
    }

    // Fallback methods

    private GatewayAuthorizationResponse authorizeFallback(
            String cardToken,
            BigDecimal amount,
            String currency,
            String reference,
            Exception e) {

        log.error("Razorpay authorization fallback triggered - Reference: {}, Error: {}",
                reference, e.getMessage());

        return GatewayAuthorizationResponse.builder()
                .success(false)
                .errorMessage("Razorpay gateway temporarily unavailable. Please try again later.")
                .errorCode("GATEWAY_UNAVAILABLE")
                .build();
    }

    private GatewayCaptureResponse captureFallback(
            String paymentId,
            BigDecimal amount,
            Exception e) {

        log.error("Razorpay capture fallback triggered - PaymentId: {}, Error: {}",
                paymentId, e.getMessage());

        return GatewayCaptureResponse.builder()
                .success(false)
                .errorMessage("Razorpay gateway temporarily unavailable. Please try again later.")
                .errorCode("GATEWAY_UNAVAILABLE")
                .build();
    }
}