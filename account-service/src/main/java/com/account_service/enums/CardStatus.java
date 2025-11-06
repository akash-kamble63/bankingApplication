package com.account_service.enums;

public enum CardStatus {
	PENDING_ACTIVATION,  // Card issued but not activated
    ACTIVE,              // Card is active and can be used
    BLOCKED,             // Temporarily blocked by user/bank
    EXPIRED,             // Card has expired
    LOST,                // Card reported as lost
    STOLEN,              // Card reported as stolen
    DAMAGED,             // Card is damaged and needs replacement
    CANCELLED,           // Card permanently cancelled
    REPLACED             // Card has been replaced with a new one
}
