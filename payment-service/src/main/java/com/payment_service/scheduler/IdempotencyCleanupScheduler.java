package com.payment_service.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.payment_service.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled jobs for idempotency key cleanup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {
    private final IdempotencyService idempotencyService;

    /**
     * Clean up expired idempotency keys every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupExpiredKeys() {
        log.info("Starting expired idempotency keys cleanup");
        try {
            int deleted = idempotencyService.cleanupExpiredKeys();
            log.info("Cleanup completed: {} keys deleted", deleted);
        } catch (Exception e) {
            log.error("Error during idempotency keys cleanup", e);
        }
    }

    /**
     * Clean up stuck processing requests every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void cleanupStuckRequests() {
        log.info("Starting stuck requests cleanup");
        try {
            idempotencyService.cleanupStuckRequests();
            log.info("Stuck requests cleanup completed");
        } catch (Exception e) {
            log.error("Error during stuck requests cleanup", e);
        }
    }
}
