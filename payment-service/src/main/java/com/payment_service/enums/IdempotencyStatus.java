package com.payment_service.enums;

public enum IdempotencyStatus {
    /**
     * Request is currently being processed
     */
    PROCESSING,

    /**
     * Request completed successfully
     */
    COMPLETED,

    /**
     * Request failed
     */
    FAILED,

    /**
     * Request expired (older than 24 hours)
     */
    EXPIRED
}
