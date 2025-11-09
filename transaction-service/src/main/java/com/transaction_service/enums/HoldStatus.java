package com.transaction_service.enums;

public enum HoldStatus {
	ACTIVE,           // Hold is active
    RELEASED,         // Hold manually released
    EXPIRED,          // Hold auto-expired
    CANCELLED,        // Hold cancelled before expiry
    COMPLETED         // Hold completed after transaction
}
