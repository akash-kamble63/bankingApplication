package com.payment_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.entity.IdempotencyKey;
import com.payment_service.enums.IdempotencyStatus;

import com.payment_service.exception.IdempotencyException;
import com.payment_service.exception.IdempotencyConflictException;
import com.payment_service.repository.IdempotencyKeyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling idempotency in payment operations.
 * Prevents duplicate payment processing by tracking request fingerprints.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final int IDEMPOTENCY_KEY_EXPIRY_HOURS = 24;
    private static final int MAX_CONCURRENT_REQUESTS_PER_USER = 10;
    private static final int PROCESSING_TIMEOUT_MINUTES = 5;

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create or retrieve an idempotency key.
     * Returns existing key if found, creates new one if not.
     * 
     * @throws IdempotencyConflictException if request body doesn't match existing
     *                                      key
     * @throws IdempotencyException         if too many concurrent requests
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey createOrGet(
            String idempotencyKey,
            Long userId,
            String requestPath,
            String requestMethod,
            Object requestBody,
            String ipAddress,
            String userAgent) {

        log.debug("Processing idempotency key: {} for user: {}", idempotencyKey, userId);

        // Check for existing key
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
                .findByIdempotencyKeyAndUserId(idempotencyKey, userId);

        String requestHash = hashRequest(requestBody);

        if (existing.isPresent()) {
            IdempotencyKey existingKey = existing.get();

            // Check if expired
            if (existingKey.isExpired()) {
                log.warn("Idempotency key expired: {}", idempotencyKey);
                // Delete expired and create new
                idempotencyKeyRepository.delete(existingKey);
                return createNew(idempotencyKey, userId, requestPath, requestMethod,
                        requestHash, ipAddress, userAgent);
            }

            // Verify request body matches
            if (!existingKey.getRequestHash().equals(requestHash)) {
                log.error("Request body mismatch for idempotency key: {}", idempotencyKey);
                throw new IdempotencyConflictException(
                        "Request body does not match original request for this idempotency key");
            }

            log.debug("Found existing idempotency key with status: {}", existingKey.getStatus());
            return existingKey;
        }

        // Check rate limit
        checkConcurrentRequestLimit(userId);

        // Create new key
        return createNew(idempotencyKey, userId, requestPath, requestMethod,
                requestHash, ipAddress, userAgent);
    }

    /**
     * Mark idempotency key as completed with response
     */
    @Transactional
    public void markCompleted(String idempotencyKey, String paymentReference,
            int responseCode, Object responseBody) {
        log.debug("Marking idempotency key as completed: {}", idempotencyKey);

        idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(key -> {
                    key.setStatus(IdempotencyStatus.COMPLETED);
                    key.setPaymentReference(paymentReference);
                    key.setResponseCode(responseCode);
                    key.setResponseBody(serializeResponse(responseBody));
                    key.setCompletedAt(LocalDateTime.now());
                    idempotencyKeyRepository.save(key);
                });
    }

    /**
     * Mark idempotency key as failed
     */
    @Transactional
    public void markFailed(String idempotencyKey, String errorMessage, int responseCode) {
        log.debug("Marking idempotency key as failed: {}", idempotencyKey);

        idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(key -> {
                    key.setStatus(IdempotencyStatus.FAILED);
                    key.setErrorMessage(errorMessage);
                    key.setResponseCode(responseCode);
                    key.setCompletedAt(LocalDateTime.now());
                    idempotencyKeyRepository.save(key);
                });
    }

    /**
     * Get cached response if available
     */
    public Optional<String> getCachedResponse(String idempotencyKey) {
        return idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                .filter(IdempotencyKey::isCompleted)
                .map(IdempotencyKey::getResponseBody);
    }

    /**
     * Check if request is currently being processed
     */
    public boolean isProcessing(String idempotencyKey) {
        return idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                .map(IdempotencyKey::isProcessing)
                .orElse(false);
    }

    /**
     * Clean up expired idempotency keys (scheduled job)
     */
    @Transactional
    public int cleanupExpiredKeys() {
        log.info("Cleaning up expired idempotency keys");
        int deleted = idempotencyKeyRepository.deleteExpiredKeys(LocalDateTime.now());
        log.info("Deleted {} expired idempotency keys", deleted);
        return deleted;
    }

    /**
     * Clean up stuck processing requests (scheduled job)
     */
    @Transactional
    public void cleanupStuckRequests() {
        log.info("Cleaning up stuck processing requests");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        var stuckKeys = idempotencyKeyRepository.findStuckProcessingKeys(
                IdempotencyStatus.PROCESSING, threshold);

        stuckKeys.forEach(key -> {
            log.warn("Found stuck processing request: {}", key.getIdempotencyKey());
            key.setStatus(IdempotencyStatus.FAILED);
            key.setErrorMessage("Request timeout - processing took too long");
            key.setCompletedAt(LocalDateTime.now());
            idempotencyKeyRepository.save(key);
        });

        log.info("Cleaned up {} stuck requests", stuckKeys.size());
    }

    // Private helper methods

    private IdempotencyKey createNew(
            String idempotencyKey,
            Long userId,
            String requestPath,
            String requestMethod,
            String requestHash,
            String ipAddress,
            String userAgent) {

        IdempotencyKey newKey = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .requestPath(requestPath)
                .requestMethod(requestMethod)
                .requestHash(requestHash)
                .status(IdempotencyStatus.PROCESSING)
                .expiresAt(LocalDateTime.now().plusHours(IDEMPOTENCY_KEY_EXPIRY_HOURS))
                .requestIp(ipAddress)
                .userAgent(userAgent)
                .build();

        try {
            return idempotencyKeyRepository.save(newKey);
        } catch (DataIntegrityViolationException e) {
            // Race condition - another thread created it
            log.debug("Concurrent creation detected for key: {}", idempotencyKey);
            return idempotencyKeyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId)
                    .orElseThrow(() -> new IdempotencyException("Failed to create or retrieve idempotency key"));
        }
    }

    private void checkConcurrentRequestLimit(Long userId) {
        long processingCount = idempotencyKeyRepository
                .countByUserIdAndStatus(userId, IdempotencyStatus.PROCESSING);

        if (processingCount >= MAX_CONCURRENT_REQUESTS_PER_USER) {
            log.error("User {} exceeded concurrent request limit: {}", userId, processingCount);
            throw new IdempotencyException(
                    "Too many concurrent payment requests. Please wait for existing requests to complete.");
        }
    }

    private String hashRequest(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to hash request", e);
            throw new IdempotencyException("Failed to process request", e);
        }
    }

    private String serializeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
            return null;
        }
    }
}
