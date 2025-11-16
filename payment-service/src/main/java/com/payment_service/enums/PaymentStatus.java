package com.payment_service.enums;

public enum PaymentStatus {
	INITIATED,              // Payment created
    VALIDATING,             // Validating payment details
    FRAUD_CHECK_PENDING,    // Sent to fraud detection
    FRAUD_APPROVED,         // Fraud check passed
    FRAUD_REJECTED,         // Blocked by fraud
    PENDING_AUTHORIZATION,  // Waiting for user authorization (OTP)
    AUTHORIZED,             // User authorized (OTP verified)
    PROCESSING,             // Being processed by gateway
    CAPTURED,               // Amount captured by gateway
    COMPLETED,              // Successfully completed
    FAILED,                 // Failed
    EXPIRED,                // Expired (timeout)
    REFUNDED,               // Full refund
    PARTIALLY_REFUNDED,     // Partial refund
    REVERSED,               // Reversed/cancelled
    DISPUTED,               // Chargeback dispute
    CANCELLED               // User cancelled
}
