package com.payment_service.clients;


import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceClient {
	private final WebClient merchantServiceWebClient;

    @CircuitBreaker(name = "merchant-service")
    public void creditMerchant(Long merchantId, BigDecimal amount, String reference) {
        log.debug("Crediting merchant: {} amount: {}", merchantId, amount);
        
        merchantServiceWebClient.post()
            .uri("/api/v1/merchants/{id}/credit", merchantId)
            .bodyValue(Map.of(
                "amount", amount,
                "reference", reference
            ))
            .retrieve()
            .bodyToMono(Void.class)
            .block(Duration.ofSeconds(5));
    }

    @CircuitBreaker(name = "merchant-service")
    public void reverseMerchantCredit(Long merchantId, BigDecimal amount, String reference) {
        log.debug("Reversing merchant credit: {} amount: {}", merchantId, amount);
        
        merchantServiceWebClient.post()
            .uri("/api/v1/merchants/{id}/reverse", merchantId)
            .bodyValue(Map.of(
                "amount", amount,
                "reference", reference
            ))
            .retrieve()
            .bodyToMono(Void.class)
            .block(Duration.ofSeconds(5));
    }
}
