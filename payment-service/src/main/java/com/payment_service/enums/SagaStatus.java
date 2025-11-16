package com.payment_service.enums;

public enum SagaStatus {
	STARTED,
    PROCESSING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
