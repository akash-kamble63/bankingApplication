package com.transaction_service.service;

import com.transaction_service.enums.AuditAction;

public interface AuditService {
	 void logSuccess(AuditAction action, Long userId, String entityType, 
             String entityId, Object details);
void logFailure(AuditAction action, Long userId, String entityType, 
             String entityId, Object details, String errorMessage);
}
