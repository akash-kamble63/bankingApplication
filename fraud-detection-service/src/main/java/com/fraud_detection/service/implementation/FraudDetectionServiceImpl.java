package com.fraud_detection.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud_detection.dto.FraudCheckDetailDTO;
import com.fraud_detection.dto.FraudCheckRequestDTO;
import com.fraud_detection.dto.FraudCheckResponseDTO;
import com.fraud_detection.dto.FraudEventDTO;
import com.fraud_detection.dto.FraudReviewRequestDTO;
import com.fraud_detection.dto.FraudStatisticsDTO;
import com.fraud_detection.dto.TransactionEventDTO;
import com.fraud_detection.entity.FraudCheck;
import com.fraud_detection.enums.FraudStatus;
import com.fraud_detection.repository.FraudCheckRepository;
import com.fraud_detection.rules.FraudRulesEngine;
import com.fraud_detection.rules.RuleCheckResult;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl {
	private final FraudCheckRepository fraudCheckRepository;
    private final FraudRulesEngine fraudRulesEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    @CircuitBreaker(name = "fraudDetection", fallbackMethod = "checkFraudFallback")
    @RateLimiter(name = "fraudDetection")
    public FraudCheckResponseDTO checkFraud(FraudCheckRequestDTO request) {
        log.info("Starting fraud check for transaction: {}", request.getTransactionId());
        
        // Check for idempotency
        String idempotencyKey = "fraud_check:" + request.getTransactionId();
        FraudCheckResponseDTO cachedResponse = (FraudCheckResponseDTO) 
                redisTemplate.opsForValue().get(idempotencyKey);
        
        if (cachedResponse != null) {
            log.info("Returning cached fraud check result for transaction: {}", 
                    request.getTransactionId());
            return cachedResponse;
        }
        
        // Convert to TransactionEventDTO
        TransactionEventDTO transaction = TransactionEventDTO.builder()
                .transactionId(request.getTransactionId())
                .accountId(request.getAccountId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionType(request.getTransactionType())
                .merchantName(request.getMerchantName())
                .merchantCategory(request.getMerchantCategory())
                .locationCountry(request.getLocationCountry())
                .locationCity(request.getLocationCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deviceId(request.getDeviceId())
                .ipAddress(request.getIpAddress())
                .timestamp(LocalDateTime.now())
                .build();
        
        // Run fraud rules engine
        RuleCheckResult ruleResult = fraudRulesEngine.evaluateTransaction(transaction);
        
        // Determine fraud status based on risk score
        FraudStatus status = determineFraudStatus(ruleResult.getRiskScore());
        
        // Save fraud check result
        FraudCheck fraudCheck = FraudCheck.builder()
                .transactionId(request.getTransactionId())
                .accountId(request.getAccountId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(status)
                .riskScore(ruleResult.getRiskScore())
                .fraudReasons(String.join("; ", ruleResult.getViolations()))
                .merchantName(request.getMerchantName())
                .merchantCategory(request.getMerchantCategory())
                .transactionType(request.getTransactionType())
                .locationCountry(request.getLocationCountry())
                .locationCity(request.getLocationCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deviceId(request.getDeviceId())
                .ipAddress(request.getIpAddress())
                .build();
        
        fraudCheck = fraudCheckRepository.save(fraudCheck);
        
        // Publish event directly to Kafka (NO OUTBOX)
        publishFraudEventToKafka(fraudCheck);
        
        // Update user profile statistics in Redis
        updateUserStatistics(request.getAccountId(), request.getAmount());
        
        // Build response
        FraudCheckResponseDTO response = FraudCheckResponseDTO.builder()
                .transactionId(request.getTransactionId())
                .status(status)
                .riskScore(ruleResult.getRiskScore())
                .fraudReasons(ruleResult.getViolations())
                .recommendation(getRecommendation(status))
                .checkedAt(LocalDateTime.now())
                .build();
        
        // Cache response for idempotency (24 hours)
        redisTemplate.opsForValue().set(idempotencyKey, response, 24, TimeUnit.HOURS);
        
        log.info("Fraud check completed for transaction: {} with status: {} and risk score: {}", 
                request.getTransactionId(), status, ruleResult.getRiskScore());
        
        return response;
    }
    
    private FraudStatus determineFraudStatus(double riskScore) {
        if (riskScore >= 0.7) {
            return FraudStatus.BLOCKED;
        } else if (riskScore >= 0.5) {
            return FraudStatus.MANUAL_REVIEW;
        } else if (riskScore >= 0.3) {
            return FraudStatus.FLAGGED;
        } else {
            return FraudStatus.APPROVED;
        }
    }
    
    private String getRecommendation(FraudStatus status) {
        return switch (status) {
            case APPROVED -> "Transaction approved - low fraud risk";
            case FLAGGED -> "Transaction flagged - monitor for suspicious activity";
            case MANUAL_REVIEW -> "Manual review required - elevated fraud risk";
            case BLOCKED -> "Transaction blocked - high fraud risk detected";
        };
    }
    
    /**
     * Publish fraud event directly to Kafka (without outbox pattern)
     * Fraud detection is a read-only/analysis service, so eventual consistency is acceptable
     */
    private void publishFraudEventToKafka(FraudCheck fraudCheck) {
        try {
            FraudEventDTO eventDTO = FraudEventDTO.builder()
                    .transactionId(fraudCheck.getTransactionId())
                    .accountId(fraudCheck.getAccountId())
                    .userId(fraudCheck.getUserId())
                    .status(fraudCheck.getStatus())
                    .riskScore(fraudCheck.getRiskScore())
                    .fraudReasons(fraudCheck.getFraudReasons() != null ? 
                            Arrays.asList(fraudCheck.getFraudReasons().split("; ")) : List.of())
                    .eventTime(LocalDateTime.now())
                    .eventType("FRAUD_CHECK_COMPLETED")
                    .build();
            
            String payload = objectMapper.writeValueAsString(eventDTO);
            
            // Publish directly to Kafka
            kafkaTemplate.send("banking.fraud.events", fraudCheck.getTransactionId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud event to Kafka for transaction: {}", 
                            fraudCheck.getTransactionId(), ex);
                    } else {
                        log.debug("Fraud event published to Kafka: transaction={}", 
                            fraudCheck.getTransactionId());
                    }
                });
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing fraud event", e);
        }
    }
    
    private void updateUserStatistics(String accountId, BigDecimal amount) {
        String avgKey = "avg_amount:" + accountId;
        String countKey = "txn_count:" + accountId;
        
        // Update average transaction amount
        Double currentAvg = (Double) redisTemplate.opsForValue().get(avgKey);
        Long count = redisTemplate.opsForValue().increment(countKey);
        
        if (currentAvg == null) {
            currentAvg = amount.doubleValue();
        } else if (count != null) {
            currentAvg = ((currentAvg * (count - 1)) + amount.doubleValue()) / count;
        }
        
        redisTemplate.opsForValue().set(avgKey, currentAvg, 30, TimeUnit.DAYS);
    }
    
    @Transactional
    public FraudCheckDetailDTO reviewFraudCheck(Long fraudCheckId, 
                                                 FraudReviewRequestDTO reviewRequest, 
                                                 String reviewerUserId) {
        log.info("Reviewing fraud check: {} by user: {}", fraudCheckId, reviewerUserId);
        
        FraudCheck fraudCheck = fraudCheckRepository.findById(fraudCheckId)
                .orElseThrow(() -> new RuntimeException("Fraud check not found: " + fraudCheckId));
        
        // Update review information
        fraudCheck.setReviewed(true);
        fraudCheck.setReviewedBy(reviewerUserId);
        fraudCheck.setReviewedAt(LocalDateTime.now());
        fraudCheck.setReviewNotes(reviewRequest.getNotes());
        
        // Update status based on review decision
        switch (reviewRequest.getDecision()) {
            case APPROVE -> fraudCheck.setStatus(FraudStatus.APPROVED);
            case REJECT -> fraudCheck.setStatus(FraudStatus.BLOCKED);
            case REQUEST_MORE_INFO -> {} // Keep current status
        }
        
        fraudCheck = fraudCheckRepository.save(fraudCheck);
        
        // Publish review event directly to Kafka
        publishFraudEventToKafka(fraudCheck);
        
        return convertToDetailDTO(fraudCheck);
    }
    
    @Cacheable(value = "fraudCheck", key = "#transactionId")
    public FraudCheckDetailDTO getFraudCheckByTransaction(String transactionId) {
        FraudCheck fraudCheck = fraudCheckRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Fraud check not found for transaction: " + transactionId));
        return convertToDetailDTO(fraudCheck);
    }
    
    public Page<FraudCheckDetailDTO> getFraudChecksByStatus(FraudStatus status, Pageable pageable) {
        return fraudCheckRepository.findByStatus(status, pageable)
                .map(this::convertToDetailDTO);
    }
    
    public Page<FraudCheckDetailDTO> getPendingReviews(Pageable pageable) {
        return fraudCheckRepository.findByReviewedFalseAndStatus(FraudStatus.MANUAL_REVIEW, pageable)
                .map(this::convertToDetailDTO);
    }
    
    public Page<FraudCheckDetailDTO> getFraudChecksByAccount(String accountId, Pageable pageable) {
        return fraudCheckRepository.findByAccountId(accountId, pageable)
                .map(this::convertToDetailDTO);
    }
    
    public FraudStatisticsDTO getStatistics(LocalDateTime since) {
        LocalDateTime startTime = since != null ? since : LocalDateTime.now().minusDays(30);
        
        Long totalChecks = fraudCheckRepository.count();
        Long approvedCount = fraudCheckRepository.countByStatusSince(FraudStatus.APPROVED, startTime);
        Long flaggedCount = fraudCheckRepository.countByStatusSince(FraudStatus.FLAGGED, startTime);
        Long blockedCount = fraudCheckRepository.countByStatusSince(FraudStatus.BLOCKED, startTime);
        Long manualReviewCount = fraudCheckRepository.countByStatusSince(FraudStatus.MANUAL_REVIEW, startTime);
        Double avgRiskScore = fraudCheckRepository.averageRiskScoreSince(startTime);
        
        return FraudStatisticsDTO.builder()
                .totalChecks(totalChecks)
                .approvedCount(approvedCount)
                .flaggedCount(flaggedCount)
                .blockedCount(blockedCount)
                .manualReviewCount(manualReviewCount)
                .averageRiskScore(avgRiskScore != null ? avgRiskScore : 0.0)
                .build();
    }
    
    private FraudCheckDetailDTO convertToDetailDTO(FraudCheck fraudCheck) {
        List<String> reasons = fraudCheck.getFraudReasons() != null && !fraudCheck.getFraudReasons().isEmpty() ?
                Arrays.asList(fraudCheck.getFraudReasons().split("; ")) : List.of();
        
        return FraudCheckDetailDTO.builder()
                .id(fraudCheck.getId())
                .transactionId(fraudCheck.getTransactionId())
                .accountId(fraudCheck.getAccountId())
                .userId(fraudCheck.getUserId())
                .amount(fraudCheck.getAmount())
                .currency(fraudCheck.getCurrency())
                .status(fraudCheck.getStatus())
                .riskScore(fraudCheck.getRiskScore())
                .fraudReasons(reasons)
                .merchantName(fraudCheck.getMerchantName())
                .merchantCategory(fraudCheck.getMerchantCategory())
                .transactionType(fraudCheck.getTransactionType())
                .locationCountry(fraudCheck.getLocationCountry())
                .locationCity(fraudCheck.getLocationCity())
                .reviewed(fraudCheck.getReviewed())
                .reviewedBy(fraudCheck.getReviewedBy())
                .reviewedAt(fraudCheck.getReviewedAt())
                .reviewNotes(fraudCheck.getReviewNotes())
                .createdAt(fraudCheck.getCreatedAt())
                .build();
    }
    
    // Fallback method for circuit breaker
    public FraudCheckResponseDTO checkFraudFallback(FraudCheckRequestDTO request, Exception ex) {
        log.error("Fraud check service unavailable, using fallback for transaction: {}", 
                request.getTransactionId(), ex);
        
        return FraudCheckResponseDTO.builder()
                .transactionId(request.getTransactionId())
                .status(FraudStatus.MANUAL_REVIEW)
                .riskScore(0.5)
                .fraudReasons(List.of("Service temporarily unavailable - manual review required"))
                .recommendation("Manual review required due to service unavailability")
                .checkedAt(LocalDateTime.now())
                .build();
    }
}
