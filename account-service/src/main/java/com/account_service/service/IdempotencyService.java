package com.account_service.service;

import java.util.Optional;

import com.account_service.model.IdempotencyRecord;

public interface IdempotencyService {
	public Optional<IdempotencyRecord> checkIdempotency(String idempotencyKey);
	public void saveIdempotencyRecord(String idempotencyKey, Object request, 
            Object response, Integer statusCode,
            String endpoint, String method, Long userId);
	public boolean createProcessingRecord(String idempotencyKey, Object request,
            String endpoint, String method, Long userId);
	public boolean verifyRequestMatch(IdempotencyRecord record, Object currentRequest);
	public int cleanupExpiredRecords();
	
}
