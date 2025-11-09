package com.transaction_service.enums;

public enum ScheduleStatus {
	ACTIVE, // Currently active and will execute
	PAUSED, // Temporarily paused
	COMPLETED, // All executions completed
	CANCELLED, // Cancelled by user
	EXPIRED, // Past end date
	FAILED // Failed to execute
}
