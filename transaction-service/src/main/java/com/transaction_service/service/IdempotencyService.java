package com.transaction_service.service;

import java.util.Optional;

import com.transaction_service.entity.IdempotencyRecord;

public interface IdempotencyService {
	Optional<IdempotencyRecord> checkIdempotency(String idempotencyKey);
    void saveIdempotencyRecord(String idempotencyKey, Object request, Object response, 
                              Integer statusCode, String endpoint, String method, Long userId);
    int cleanupExpiredRecords();
}
