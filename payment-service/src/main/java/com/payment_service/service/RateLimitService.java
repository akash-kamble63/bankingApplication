package com.payment_service.service;

public interface RateLimitService {
	boolean checkPaymentLimit(Long userId);

	void recordPaymentAttempt(Long userId, boolean success);

}
