package com.transaction_service.service.implementation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction_service.entity.IdempotencyRecord;
import com.transaction_service.repository.IdempotencyRepository;
import com.transaction_service.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {
	private final IdempotencyRepository idempotencyRepository;
	private final ObjectMapper objectMapper;
	private static final int DEFAULT_TTL_HOURS = 24;

	@Override
	@Transactional(readOnly = true)
	public Optional<IdempotencyRecord> checkIdempotency(String idempotencyKey) {
		return idempotencyRepository.findActiveByKey(idempotencyKey);
	}

	@Override
	@Transactional
	public void saveIdempotencyRecord(String idempotencyKey, Object request, Object response, Integer statusCode,
			String endpoint, String method, Long userId) {
		try {
			String requestHash = generateHash(request);
			String responseBody = objectMapper.writeValueAsString(response);

			IdempotencyRecord record = IdempotencyRecord.builder()
					.idempotencyKey(idempotencyKey)
					.requestHash(requestHash).responseStatus(statusCode).responseBody(responseBody).endpoint(endpoint)
					.httpMethod(method).userId(userId).expiresAt(LocalDateTime.now().plusHours(DEFAULT_TTL_HOURS))
					.processing(false).build();

			idempotencyRepository.save(record);

		} catch (Exception e) {
			log.error("Failed to save idempotency record: {}", e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public int cleanupExpiredRecords() {
		// Implementation for cleanup
		return 0;
	}

	private String generateHash(Object request) throws Exception {
		String json = objectMapper.writeValueAsString(request);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

		StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
