package com.payment_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payment_service.entity.IdempotencyKey;
import com.payment_service.enums.IdempotencyStatus;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    /**
     * Find idempotency key by key value
     */
    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find by key and user (for security check)
     */
    Optional<IdempotencyKey> findByIdempotencyKeyAndUserId(String idempotencyKey, Long userId);

    /**
     * Check if key exists for user
     */
    boolean existsByIdempotencyKeyAndUserId(String idempotencyKey, Long userId);

    /**
     * Find all keys for a user within time range
     */
    List<IdempotencyKey> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    /**
     * Delete expired keys (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :now")
    int deleteExpiredKeys(@Param("now") LocalDateTime now);

    /**
     * Find processing keys older than threshold (stuck requests)
     */
    @Query("SELECT i FROM IdempotencyKey i WHERE i.status = :status AND i.createdAt < :threshold")
    List<IdempotencyKey> findStuckProcessingKeys(
            @Param("status") IdempotencyStatus status,
            @Param("threshold") LocalDateTime threshold);

    /**
     * Count processing requests for a user (rate limiting)
     */
    @Query("SELECT COUNT(i) FROM IdempotencyKey i WHERE i.userId = :userId AND i.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") IdempotencyStatus status);
}