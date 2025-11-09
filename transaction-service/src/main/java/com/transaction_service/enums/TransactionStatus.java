package com.transaction_service.enums;

public enum TransactionStatus {
	INITIATED, // Transaction started
	PENDING, // Awaiting approval/processing
	PROCESSING, // Being processed
	SUCCESS, // Completed successfully
	COMPLETED, // Alternative for SUCCESS
	FAILED, // Failed
	REVERSED, // Reversed/Cancelled
	DECLINED, // Declined by system/bank
	TIMEOUT, // Timed out
	ON_HOLD, // Held for verification
	CANCELLED // Manually cancelled
}
