package com.transaction_service.enums;

public enum AuditAction {
	TRANSACTION_CREATED,
    TRANSACTION_UPDATED,
    TRANSACTION_COMPLETED,
    TRANSACTION_FAILED,
    TRANSACTION_REVERSED,
    TRANSACTION_CANCELLED,
    
    // Account actions
    ACCOUNT_ACCESSED,
    ACCOUNT_DETAILS_VIEWED,
    BALANCE_CHECKED,
    
    // Limit actions
    LIMIT_UPDATED,
    LIMIT_EXCEEDED,
    LIMIT_RESET,
    
    // Schedule actions
    SCHEDULE_CREATED,
    SCHEDULE_UPDATED,
    SCHEDULE_CANCELLED,
    SCHEDULE_EXECUTED,
    
    // Security actions
    FAILED_TRANSACTION_ATTEMPT,
    SUSPICIOUS_ACTIVITY,
    
    // Admin actions
    ADMIN_ACCESS,
    MANUAL_INTERVENTION,
    
    // System actions
    SYSTEM_ERROR,
    RETRY_ATTEMPTED
}
