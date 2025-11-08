package com.account_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.model.IdempotencyRecord;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
	Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

	@Query("SELECT i FROM IdempotencyRecord i WHERE i.idempotencyKey = :key " + "AND i.expiresAt > CURRENT_TIMESTAMP")
	Optional<IdempotencyRecord> findActiveByKey(@Param("key") String key);

	@Modifying
	@Query("UPDATE IdempotencyRecord i SET i.processing = true WHERE i.idempotencyKey = :key")
	int markAsProcessing(@Param("key") String key);

	@Modifying
	@Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :expiryDate")
	int deleteExpired(@Param("expiryDate") LocalDateTime expiryDate);

	boolean existsByIdempotencyKey(String idempotencyKey);
}
