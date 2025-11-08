package com.account_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.enums.AuditAction;
import com.account_service.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
	Page<AuditLog> findByUserId(Long userId, Pageable pageable);

	Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

	Page<AuditLog> findByUserIdAndAction(Long userId, AuditAction action, Pageable pageable);

	// Date range queries
	@Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
	Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
			Pageable pageable);

	@Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " + "AND a.createdAt BETWEEN :startDate AND :endDate")
	Page<AuditLog> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate, Pageable pageable);

	// Entity specific
	Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

	// Count operations
	long countByAction(AuditAction action);

	long countByUserIdAndAction(Long userId, AuditAction action);

	// Cleanup
	@Modifying
	@Query("DELETE FROM AuditLog a WHERE a.createdAt < :retentionDate")
	void deleteOldLogs(@Param("retentionDate") LocalDateTime retentionDate);
}
