package com.transaction_service.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.transaction_service.DTOs.TransferSagaData;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudServiceClient {
private final WebClient webClient;
    
    /**
     * Quick fraud check (sync, < 100ms)
     */
    @CircuitBreaker(name = "fraudService", fallbackMethod = "quickCheckFallback")
    @Retry(name = "fraudService")
    public FraudCheckResult quickCheck(TransferSagaData data) {
        try {
            return webClient.post()
                .uri("/api/v1/fraud/quick-check")
                .bodyValue(Map.of(
                    "transactionReference", data.getTransactionReference(),
                    "sourceAccountId", data.getSourceAccountId(),
                    "destinationAccountId", data.getDestinationAccountId(),
                    "amount", data.getAmount(),
                    "userId", data.getUserId()
                ))
                .retrieve()
                .bodyToMono(FraudCheckResult.class)
                .block(Duration.ofMillis(100)); // Fast timeout
                
        } catch (Exception e) {
            log.error("Quick fraud check failed: {}", e.getMessage());
            return quickCheckFallback(data, e);
        }
    }
    
    /**
     * Deep fraud analysis (async)
     */
    public CompletableFuture<FraudAnalysisResult> deepAnalysis(TransferSagaData data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return webClient.post()
                    .uri("/api/v1/fraud/deep-analysis")
                    .bodyValue(data)
                    .retrieve()
                    .bodyToMono(FraudAnalysisResult.class)
                    .block(Duration.ofSeconds(5));
                    
            } catch (Exception e) {
                log.error("Deep fraud analysis failed: {}", e.getMessage());
                return null;
            }
        });
    }
    
    private FraudCheckResult quickCheckFallback(TransferSagaData data, Exception e) {
        log.warn("Fraud service unavailable, using fallback: {}", e.getMessage());
        
        // Simple rule-based check as fallback
        boolean blocked = data.getAmount().compareTo(new BigDecimal("100000")) > 0;
        
        return FraudCheckResult.builder()
            .transactionReference(data.getTransactionReference())
            .fraudScore(blocked ? new BigDecimal("80") : new BigDecimal("10"))
            .riskLevel(blocked ? "HIGH" : "LOW")
            .blocked(blocked)
            .reason(blocked ? "Amount exceeds limit - Fraud service unavailable" : "OK")
            .build();
    }
}
