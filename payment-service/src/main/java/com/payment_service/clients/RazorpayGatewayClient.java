package com.payment_service.clients;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.DTOs.RazorpayAuthRequest;
import com.payment_service.DTOs.RazorpayAuthResponse;
import com.payment_service.DTOs.RazorpayCaptureResponse;
import com.payment_service.exception.PaymentGatewayException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayGatewayClient {
private final WebClient webClient;
    
    @CircuitBreaker(name = "razorpay", fallbackMethod = "authorizeFallback")
    @Retry(name = "razorpay")
    public GatewayAuthorizationResponse authorize(
            String cardToken,
            BigDecimal amount,
            String currency,
            String reference) {
        
        log.debug("Authorizing payment with Razorpay: {}", reference);
        
        try {
            RazorpayAuthRequest request = RazorpayAuthRequest.builder()
                .token(cardToken)
                .amount(amount.multiply(new BigDecimal("100")).longValue()) // Paise
                .currency(currency)
                .receipt(reference)
                .build();
            
            RazorpayAuthResponse response = webClient.post()
                .uri("https://api.razorpay.com/v1/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RazorpayAuthResponse.class)
                .block(Duration.ofSeconds(10));
            
            return GatewayAuthorizationResponse.builder()
                .success(response.getStatus().equals("authorized"))
                .gatewayPaymentId(response.getId())
                .authorizationCode(response.getAuthCode())
                .build();
                
        } catch (Exception e) {
            log.error("Razorpay authorization failed: {}", e.getMessage());
            return authorizeFallback(cardToken, amount, currency, reference, e);
        }
    }
    
    @CircuitBreaker(name = "razorpay")
    public GatewayCaptureResponse capture(String paymentId, BigDecimal amount) {
        log.debug("Capturing payment with Razorpay: {}", paymentId);
        
        try {
            RazorpayCaptureResponse response = webClient.post()
                .uri("https://api.razorpay.com/v1/payments/{id}/capture", paymentId)
                .bodyValue(Map.of("amount", amount.multiply(new BigDecimal("100")).longValue()))
                .retrieve()
                .bodyToMono(RazorpayCaptureResponse.class)
                .block(Duration.ofSeconds(10));
            
            return GatewayCaptureResponse.builder()
                .success(response.getStatus().equals("captured"))
                .transactionId(response.getId())
                .build();
                
        } catch (Exception e) {
            log.error("Razorpay capture failed: {}", e.getMessage());
            throw new PaymentGatewayException("Capture failed", e);
        }
    }
    
    public void refund(String paymentId, BigDecimal amount) {
        // Implement refund logic
    }
    
    public void voidAuth(String paymentId) {
        // Implement void logic
    }
    
    private GatewayAuthorizationResponse authorizeFallback(
            String cardToken, BigDecimal amount, String currency, 
            String reference, Exception e) {
        log.error("Razorpay fallback triggered: {}", e.getMessage());
        return GatewayAuthorizationResponse.builder()
            .success(false)
            .errorMessage("Gateway temporarily unavailable")
            .errorCode("GATEWAY_ERROR")
            .build();
    }
}
