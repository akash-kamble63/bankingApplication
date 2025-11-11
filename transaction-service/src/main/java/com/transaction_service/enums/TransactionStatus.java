package com.transaction_service.enums;

public enum TransactionStatus {
	INITIATED,          // Transaction created
    FRAUD_CHECK_PENDING,// Sent to fraud service
    FRAUD_APPROVED,     // Fraud check passed
    FRAUD_REJECTED,     // Blocked by fraud
    FUNDS_RESERVED,     // Hold placed on source account
    PROCESSING,         // Saga in progress
    COMPLETED,          // Successfully completed
    FAILED,             // Failed (with reason)
    REVERSED,           // Reversed/refunded
    CANCELLED           // User cancelled
}
