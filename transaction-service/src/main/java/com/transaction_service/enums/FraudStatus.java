package com.transaction_service.enums;

public enum FraudStatus {
	CLEAN, // No fraud detected
	SUSPICIOUS, // Flagged as suspicious
	CONFIRMED_FRAUD, // Confirmed fraud
	UNDER_REVIEW, // Under investigation
	FALSE_POSITIVE // False alarm
}
