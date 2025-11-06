package com.account_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.account_service.enums.AuditAction;
import com.account_service.model.AuditLog;

public interface AuditService {
	void logAudit(AuditAction action, Long userId, String entityType, String entityId, Object details, String status);

	void logSuccess(AuditAction action, Long userId, String entityType, String entityId, Object details);

	void logFailure(AuditAction action, Long userId, String entityType, String entityId, Object details,
			String errorMessage);

	Page<AuditLog> getAuditLogsForUser(Long userId, Pageable pageable);

	Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable);

	Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

	void cleanupOldLogs(int retentionDays);

}
