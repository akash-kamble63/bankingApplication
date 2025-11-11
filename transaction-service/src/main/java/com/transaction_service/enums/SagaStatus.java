package com.transaction_service.enums;

public enum SagaStatus {
	STARTED,
    PROCESSING,
    COMPENSATING,
    COMPLETED,
    FAILED,
    COMPENSATED
}
