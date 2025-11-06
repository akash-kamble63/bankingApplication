package com.account_service.enums;

public enum HoldStatus {
	ACTIVE,      // Hold is active
    RELEASED,    // Hold manually released
    EXPIRED,     // Hold auto-expired
    CANCELLED    // Hold cancelled before expiry
}
