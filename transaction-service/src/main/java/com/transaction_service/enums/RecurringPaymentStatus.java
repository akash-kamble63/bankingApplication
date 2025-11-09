package com.transaction_service.enums;

public enum RecurringPaymentStatus {
	ACTIVE, // Recurring payment active
	PAUSED, // Temporarily paused
	CANCELLED, // Cancelled
	FAILED, // Failed to execute
	EXPIRED // Expired
}
