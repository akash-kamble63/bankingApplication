package com.account_service.enums;

public enum AccountStatus {
	ACTIVE, // Normal operation
	INACTIVE, // Temporarily inactive
	FROZEN, // Frozen due to suspicious activity
	CLOSED, // Permanently closed
	DORMANT // Inactive for long period
}
