package com.account_service.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.account_service.dto.BeneficiaryResponse;
import com.account_service.dto.CreateBeneficiaryRequest;
import com.account_service.enums.BeneficiaryStatus;
import com.account_service.exception.ResourceConflictException;
import com.account_service.exception.ResourceNotFoundException;
import com.account_service.kafka.BeneficiaryEventPublisher;
import com.account_service.model.Beneficiary;
import com.account_service.model.OutboxEvent;
import com.account_service.patterns.BeneficiaryMetrics;
import com.account_service.repository.BeneficiaryRepository;
import com.account_service.repository.OutboxRepository;
import com.account_service.service.BeneficiaryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final OutboxRepository outboxRepository;
    private final BeneficiaryEventPublisher eventPublisher;
    private final BeneficiaryMetrics metrics;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String BENEFICIARY_CACHE = "beneficiaries";
    private static final String USER_BENEFICIARIES_CACHE = "userBeneficiaries";
    private static final String IDEMPOTENCY_PREFIX = "beneficiary:idempotency:";
    private static final int IDEMPOTENCY_TTL_HOURS = 24;
    private static final int MAX_BENEFICIARIES_PER_USER = 50;

    @Override
    @Transactional
    @CacheEvict(value = USER_BENEFICIARIES_CACHE, key = "#request.userId")
    public BeneficiaryResponse addBeneficiary(CreateBeneficiaryRequest request) {
        Timer.Sample sample = metrics.startTimer();

        try {
            log.info("Adding beneficiary for user: {}", request.getUserId());

            // Idempotency check using Redis
            String idempotencyKey = generateIdempotencyKey(request);
            BeneficiaryResponse cachedResponse = checkIdempotency(idempotencyKey);
            if (cachedResponse != null) {
                log.info("Returning cached beneficiary response for idempotency key: {}", idempotencyKey);
                metrics.recordIdempotencyCheck(true);
                return cachedResponse;
            }
            metrics.recordIdempotencyCheck(false);

            // Check for duplicate beneficiary
            if (beneficiaryRepository.existsByUserIdAndBeneficiaryAccountNumber(
                    request.getUserId(), request.getBeneficiaryAccountNumber())) {
                metrics.recordDuplicateBeneficiaryAttempt();
                throw new ResourceConflictException("Beneficiary with account number "
                        + request.getBeneficiaryAccountNumber() + " already exists for this user");
            }

            // Validate account limit
            long beneficiaryCount = beneficiaryRepository.countByUserId(request.getUserId());
            if (beneficiaryCount >= MAX_BENEFICIARIES_PER_USER) {
                metrics.recordBeneficiaryLimitExceeded();
                throw new ResourceConflictException("Maximum beneficiary limit ("
                        + MAX_BENEFICIARIES_PER_USER + ") reached for user");
            }

            Beneficiary beneficiary = Beneficiary.builder()
                    .userId(request.getUserId())
                    .accountId(request.getAccountId())
                    .beneficiaryName(request.getBeneficiaryName())
                    .beneficiaryAccountNumber(request.getBeneficiaryAccountNumber())
                    .beneficiaryIfsc(request.getBeneficiaryIfsc())
                    .beneficiaryBank(request.getBeneficiaryBank())
                    .nickname(request.getNickname())
                    .status(BeneficiaryStatus.PENDING_VERIFICATION)
                    .isVerified(false)
                    .build();

            beneficiary = beneficiaryRepository.save(beneficiary);

            // Publish event to Kafka via Outbox pattern
            publishBeneficiaryAddedEvent(beneficiary);

            BeneficiaryResponse response = mapToResponse(beneficiary);

            // Store in Redis for idempotency
            storeIdempotencyResponse(idempotencyKey, response);

            metrics.recordBeneficiaryCreation(true);
            metrics.recordOperationDuration(sample, "addBeneficiary", true);
            log.info("Beneficiary added successfully: {}", beneficiary.getId());

            return response;

        } catch (Exception e) {
            metrics.recordBeneficiaryCreation(false);
            metrics.recordOperationDuration(sample, "addBeneficiary", false);
            log.error("Failed to add beneficiary: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USER_BENEFICIARIES_CACHE, key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<BeneficiaryResponse> getUserBeneficiaries(Long userId) {
        Timer.Sample sample = metrics.startTimer();

        try {
            log.debug("Fetching beneficiaries for user: {}", userId);

            List<Beneficiary> beneficiaries = beneficiaryRepository.findByUserId(userId);

            if (beneficiaries.isEmpty()) {
                log.debug("No beneficiaries found for user: {}", userId);
                metrics.recordCacheOperation("getUserBeneficiaries", false);
            } else {
                metrics.recordCacheOperation("getUserBeneficiaries", true);
            }

            metrics.recordDatabaseQuery(sample, "getUserBeneficiaries");

            return beneficiaries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching beneficiaries for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = BENEFICIARY_CACHE, key = "#beneficiaryId"),
            @CacheEvict(value = USER_BENEFICIARIES_CACHE, allEntries = true)
    })
    public BeneficiaryResponse verifyBeneficiary(Long beneficiaryId) {
        Timer.Sample sample = metrics.startTimer();

        try {
            log.info("Verifying beneficiary: {}", beneficiaryId);

            Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Beneficiary not found with id: " + beneficiaryId));

            // Check if already verified
            if (Boolean.TRUE.equals(beneficiary.getIsVerified())) {
                log.warn("Beneficiary {} is already verified", beneficiaryId);
                return mapToResponse(beneficiary);
            }

            beneficiary.setIsVerified(true);
            beneficiary.setVerifiedAt(LocalDateTime.now());
            beneficiary.setStatus(BeneficiaryStatus.VERIFIED);

            beneficiary = beneficiaryRepository.save(beneficiary);

            // Publish verification event
            publishBeneficiaryVerifiedEvent(beneficiary);

            metrics.recordBeneficiaryVerification(true);
            metrics.recordOperationDuration(sample, "verifyBeneficiary", true);
            log.info("Beneficiary verified successfully: {}", beneficiaryId);

            return mapToResponse(beneficiary);

        } catch (Exception e) {
            metrics.recordBeneficiaryVerification(false);
            metrics.recordOperationDuration(sample, "verifyBeneficiary", false);
            log.error("Failed to verify beneficiary {}: {}", beneficiaryId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = BENEFICIARY_CACHE, key = "#beneficiaryId"),
            @CacheEvict(value = USER_BENEFICIARIES_CACHE, allEntries = true)
    })
    public void deleteBeneficiary(Long beneficiaryId) {
        Timer.Sample sample = metrics.startTimer();

        try {
            log.info("Deleting beneficiary: {}", beneficiaryId);

            Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Beneficiary not found with id: " + beneficiaryId));

            // Soft delete by updating status (industry best practice)
            beneficiary.setStatus(BeneficiaryStatus.BLOCKED);
            beneficiaryRepository.save(beneficiary);

            // Publish deletion event
            publishBeneficiaryDeletedEvent(beneficiary);

            metrics.recordBeneficiaryDeletion(true);
            metrics.recordOperationDuration(sample, "deleteBeneficiary", true);
            log.info("Beneficiary deleted successfully: {}", beneficiaryId);

        } catch (Exception e) {
            metrics.recordBeneficiaryDeletion(false);
            metrics.recordOperationDuration(sample, "deleteBeneficiary", false);
            log.error("Failed to delete beneficiary {}: {}", beneficiaryId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiaryResponse> searchBeneficiaries(Long userId, String searchTerm, Pageable pageable) {
        Timer.Sample sample = metrics.startTimer();

        try {
            log.debug("Searching beneficiaries for user: {} with term: '{}'", userId, searchTerm);

            // If search term is null or empty, return all active beneficiaries
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                Page<BeneficiaryResponse> result = beneficiaryRepository.findByUserId(userId, pageable)
                        .map(this::mapToResponse);
                metrics.recordBeneficiarySearch((int) result.getTotalElements());
                metrics.recordDatabaseQuery(sample, "searchBeneficiaries");
                return result;
            }

            // Sanitize search term to prevent injection
            String sanitizedSearchTerm = searchTerm.trim().replaceAll("[^a-zA-Z0-9\\s]", "");

            if (sanitizedSearchTerm.isEmpty()) {
                log.warn("Search term became empty after sanitization");
                metrics.recordValidationFailure("search_term_sanitization");
                Page<BeneficiaryResponse> result = beneficiaryRepository.findByUserId(userId, pageable)
                        .map(this::mapToResponse);
                metrics.recordBeneficiarySearch((int) result.getTotalElements());
                return result;
            }

            // Fetch all matching beneficiaries
            List<Beneficiary> allMatching = beneficiaryRepository.searchBeneficiaries(userId, sanitizedSearchTerm);

            // Convert to Page manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allMatching.size());

            // Handle edge case where start is beyond list size
            if (start >= allMatching.size()) {
                metrics.recordBeneficiarySearch(0);
                return Page.empty(pageable);
            }

            List<BeneficiaryResponse> pageContent = allMatching.subList(start, end).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            metrics.recordBeneficiarySearch(allMatching.size());
            metrics.recordDatabaseQuery(sample, "searchBeneficiaries");
            log.debug("Found {} beneficiaries matching search criteria", allMatching.size());

            return new org.springframework.data.domain.PageImpl<>(
                    pageContent,
                    pageable,
                    allMatching.size());

        } catch (Exception e) {
            log.error("Error searching beneficiaries: {}", e.getMessage(), e);
            throw e;
        }
    }

    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .userId(beneficiary.getUserId())
                .accountId(beneficiary.getAccountId())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .beneficiaryAccountNumber(beneficiary.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(beneficiary.getBeneficiaryIfsc())
                .beneficiaryBank(beneficiary.getBeneficiaryBank())
                .nickname(beneficiary.getNickname())
                .status(beneficiary.getStatus())
                .isVerified(beneficiary.getIsVerified())
                .verifiedAt(beneficiary.getVerifiedAt())
                .createdAt(beneficiary.getCreatedAt())
                .build();
    }

    private void publishBeneficiaryAddedEvent(Beneficiary beneficiary) {
        try {
            String payload = objectMapper.writeValueAsString(beneficiary);

            OutboxEvent event = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType("BENEFICIARY")
                    .aggregateId(beneficiary.getId().toString())
                    .eventType("BeneficiaryAdded")
                    .topic("banking.beneficiary.created")
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            outboxRepository.save(event);
            metrics.recordEventPublished("BeneficiaryAdded", true);
            log.debug("Outbox event created for beneficiary addition: {}", event.getEventId());

        } catch (JsonProcessingException e) {
            metrics.recordEventPublished("BeneficiaryAdded", false);
            log.error("Failed to serialize beneficiary for outbox event", e);
            throw new RuntimeException("Failed to publish beneficiary added event", e);
        }
    }

    private void publishBeneficiaryVerifiedEvent(Beneficiary beneficiary) {
        try {
            String payload = objectMapper.writeValueAsString(beneficiary);

            OutboxEvent event = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType("BENEFICIARY")
                    .aggregateId(beneficiary.getId().toString())
                    .eventType("BeneficiaryVerified")
                    .topic("banking.beneficiary.verified")
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            outboxRepository.save(event);
            metrics.recordEventPublished("BeneficiaryVerified", true);
            log.debug("Outbox event created for beneficiary verification: {}", event.getEventId());

        } catch (JsonProcessingException e) {
            metrics.recordEventPublished("BeneficiaryVerified", false);
            log.error("Failed to serialize beneficiary for outbox event", e);
            throw new RuntimeException("Failed to publish beneficiary verified event", e);
        }
    }

    private void publishBeneficiaryDeletedEvent(Beneficiary beneficiary) {
        try {
            String payload = objectMapper.writeValueAsString(beneficiary);

            OutboxEvent event = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType("BENEFICIARY")
                    .aggregateId(beneficiary.getId().toString())
                    .eventType("BeneficiaryDeleted")
                    .topic("banking.beneficiary.deleted")
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            outboxRepository.save(event);
            metrics.recordEventPublished("BeneficiaryDeleted", true);
            log.debug("Outbox event created for beneficiary deletion: {}", event.getEventId());

        } catch (JsonProcessingException e) {
            metrics.recordEventPublished("BeneficiaryDeleted", false);
            log.error("Failed to serialize beneficiary for outbox event", e);
            throw new RuntimeException("Failed to publish beneficiary deleted event", e);
        }
    }

    private String generateIdempotencyKey(CreateBeneficiaryRequest request) {
        return String.format("%s:%s:%s",
                request.getUserId(),
                request.getBeneficiaryAccountNumber(),
                request.getBeneficiaryIfsc());
    }

    private BeneficiaryResponse checkIdempotency(String idempotencyKey) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                metrics.recordCacheOperation("idempotency", true);
                return objectMapper.convertValue(cached, BeneficiaryResponse.class);
            }
            metrics.recordCacheOperation("idempotency", false);
        } catch (Exception e) {
            metrics.recordRedisFailure("idempotency_check");
            log.error("Error checking idempotency in Redis", e);
        }
        return null;
    }

    private void storeIdempotencyResponse(String idempotencyKey, BeneficiaryResponse response) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(key, response, IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Stored idempotency response in Redis with key: {}", key);
        } catch (Exception e) {
            metrics.recordRedisFailure("idempotency_store");
            log.error("Error storing idempotency response in Redis", e);
        }
    }
}