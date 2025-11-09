package com.transaction_service.enums;

public enum BeneficiaryStatus {
	PENDING_VERIFICATION, // Awaiting verification
	VERIFIED, // Verified and active
	BLOCKED, // Blocked by user/system
	INACTIVE, // Temporarily inactive
	DELETED // Deleted
}
