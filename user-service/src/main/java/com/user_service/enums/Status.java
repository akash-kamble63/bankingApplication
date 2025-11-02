package com.user_service.enums;

public enum Status {

	PENDING,        // Waiting for email verification
    ACTIVE,         // Email verified, account active
    SUSPENDED,      // Account suspended
    INACTIVE,       // Account deactivated by user
    DELETED
}
