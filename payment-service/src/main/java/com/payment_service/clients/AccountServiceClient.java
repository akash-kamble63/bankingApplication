package com.payment_service.clients;


import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;



import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.payment_service.DTOs.HoldResponse;
import com.payment_service.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceClient {
	private final WebClient accountServiceWebClient;

    @CircuitBreaker(name = "account-service", fallbackMethod = "placeHoldFallback")
    @Retry(name = "account-service")
    public HoldResponse placeHold(Long accountId, BigDecimal amount, 
                                  String description, String reference) {
        log.debug("Placing hold on account: {} for amount: {}", accountId, amount);
        
        return accountServiceWebClient.post()
            .uri("/api/v1/accounts/{id}/holds", accountId)
            .bodyValue(Map.of(
                "amount", amount,
                "description", description,
                "reference", reference
            ))
            .retrieve()
            .bodyToMono(HoldResponse.class)
            .block(Duration.ofSeconds(5));
    }

    @CircuitBreaker(name = "account-service")
    public void releaseHold(String holdReference) {
        log.debug("Releasing hold: {}", holdReference);
        
        accountServiceWebClient.delete()
            .uri("/api/v1/accounts/holds/{reference}", holdReference)
            .retrieve()
            .bodyToMono(Void.class)
            .block(Duration.ofSeconds(5));
    }

    @CircuitBreaker(name = "account-service")
    public void debitWithIdempotency(Long accountId, BigDecimal amount, 
                                     String idempotencyKey, String reference) {
        log.debug("Debiting account: {} amount: {}", accountId, amount);
        
        accountServiceWebClient.post()
            .uri("/api/v1/accounts/{id}/debit", accountId)
            .bodyValue(Map.of(
                "amount", amount,
                "idempotencyKey", idempotencyKey,
                "reference", reference
            ))
            .retrieve()
            .bodyToMono(Void.class)
            .block(Duration.ofSeconds(5));
    }

    @CircuitBreaker(name = "account-service")
    public void creditWithIdempotency(Long accountId, BigDecimal amount,
                                      String idempotencyKey, String reference) {
        log.debug("Crediting account: {} amount: {}", accountId, amount);
        
        accountServiceWebClient.post()
            .uri("/api/v1/accounts/{id}/credit", accountId)
            .bodyValue(Map.of(
                "amount", amount,
                "idempotencyKey", idempotencyKey,
                "reference", reference
            ))
            .retrieve()
            .bodyToMono(Void.class)
            .block(Duration.ofSeconds(5));
    }
    @SuppressWarnings("unused")
    private HoldResponse placeHoldFallback(Long accountId, BigDecimal amount,
                                           String description, String reference,
                                           Exception e) {
        log.error("Failed to place hold on account: {}", accountId, e);
        throw new ServiceUnavailableException("Account service unavailable");
    }
}
