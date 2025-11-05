package com.user_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.user_service.enums.AuditAction;
import com.user_service.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>{
	// Find by user
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    // Find by action
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
    
    // Find by user and action
    Page<AuditLog> findByUserIdAndAction(Long userId, AuditAction action, Pageable pageable);
    
    // Find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Find by user and date range
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Count by action
    long countByAction(AuditAction action);
    
    // Count by user and action
    long countByUserIdAndAction(Long userId, AuditAction action);
    
    // Delete old logs (retention policy)
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :retentionDate")
    void deleteOldLogs(@Param("retentionDate") LocalDateTime retentionDate);
    
    // Find failed login attempts
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'FAILED_LOGIN_ATTEMPT' " +
           "AND a.ipAddress = :ipAddress " +
           "AND a.createdAt > :since")
    List<AuditLog> findFailedLoginAttempts(
        @Param("ipAddress") String ipAddress,
        @Param("since") LocalDateTime since
    );
}
