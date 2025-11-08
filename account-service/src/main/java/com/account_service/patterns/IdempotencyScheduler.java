package com.account_service.patterns;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.account_service.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyScheduler {
	 private final IdempotencyService idempotencyService;
	    
	    /**
	     * Cleanup expired idempotency records every hour
	     */
	    @Scheduled(cron = "0 0 * * * *") // Every hour
	    public void cleanupExpiredRecords() {
	        try {
	            int deleted = idempotencyService.cleanupExpiredRecords();
	            log.info("Idempotency cleanup: {} records deleted", deleted);
	        } catch (Exception e) {
	            log.error("Error cleaning up idempotency records: {}", e.getMessage(), e);
	        }
	    }
}
