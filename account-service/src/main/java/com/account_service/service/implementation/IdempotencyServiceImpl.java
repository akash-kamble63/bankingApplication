package com.account_service.service.implementation;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.model.IdempotencyRecord;
import com.account_service.repository.IdempotencyRepository;
import com.account_service.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

	private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    
    private static final int DEFAULT_TTL_HOURS = 24;
    
    /**
     * Check if request is duplicate
     * Returns cached response if found
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> checkIdempotency(String idempotencyKey) {
        return idempotencyRepository.findActiveByKey(idempotencyKey);
    }
    
    /**
     * Save idempotency record with response
     */
    @Transactional
    public void saveIdempotencyRecord(String idempotencyKey, Object request, 
                                     Object response, Integer statusCode,
                                     String endpoint, String method, Long userId) {
        try {
            String requestHash = generateHash(request);
            String responseBody = objectMapper.writeValueAsString(response);
            
            IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .responseStatus(statusCode)
                .responseBody(responseBody)
                .endpoint(endpoint)
                .httpMethod(method)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusHours(DEFAULT_TTL_HOURS))
                .processing(false)
                .build();
            
            idempotencyRepository.save(record);
            log.debug("Idempotency record saved: key={}", idempotencyKey);
            
        } catch (Exception e) {
            log.error("Failed to save idempotency record: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create idempotency record in processing state
     * Prevents concurrent duplicate requests
     */
    @Transactional
    public boolean createProcessingRecord(String idempotencyKey, Object request,
                                         String endpoint, String method, Long userId) {
        try {
            // Check if already exists
            if (idempotencyRepository.existsByIdempotencyKey(idempotencyKey)) {
                return false;
            }
            
            String requestHash = generateHash(request);
            
            IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .endpoint(endpoint)
                .httpMethod(method)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusHours(DEFAULT_TTL_HOURS))
                .processing(true)
                .build();
            
            idempotencyRepository.save(record);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to create processing record: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify request matches original (same hash)
     */
    public boolean verifyRequestMatch(IdempotencyRecord record, Object currentRequest) {
        try {
            String currentHash = generateHash(currentRequest);
            boolean matches = record.getRequestHash().equals(currentHash);
            
            if (!matches) {
                log.warn("Idempotency key reused with different request: key={}", 
                    record.getIdempotencyKey());
            }
            
            return matches;
        } catch (Exception e) {
            log.error("Error verifying request match: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate SHA-256 hash of request
     */
    private String generateHash(Object request) throws Exception {
        String json = objectMapper.writeValueAsString(request);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Cleanup expired records
     */
    @Transactional
    public int cleanupExpiredRecords() {
        int deleted = idempotencyRepository.deleteExpired(LocalDateTime.now());
        log.info("Cleaned up {} expired idempotency records", deleted);
        return deleted;
    }

}
