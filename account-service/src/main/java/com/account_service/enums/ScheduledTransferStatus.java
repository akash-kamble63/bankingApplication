package com.account_service.enums;

public enum ScheduledTransferStatus {
	ACTIVE,          // Scheduled transfer is active
    PAUSED,          // Temporarily paused by user
    COMPLETED,       // All scheduled transfers completed
    CANCELLED,       // Cancelled by user
    FAILED,          // Last execution failed
    EXPIRED          // End date reached
}
