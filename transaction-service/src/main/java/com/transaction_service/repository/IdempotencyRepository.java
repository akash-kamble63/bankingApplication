package com.transaction_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.transaction_service.entity.IdempotencyRecord;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
	Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

	@Query("SELECT i FROM IdempotencyRecord i WHERE i.idempotencyKey = :key " + "AND i.expiresAt > CURRENT_TIMESTAMP")
	Optional<IdempotencyRecord> findActiveByKey(String key);

	boolean existsByIdempotencyKey(String idempotencyKey);
}
