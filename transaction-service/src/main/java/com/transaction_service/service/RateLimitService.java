package com.transaction_service.service;

import java.time.Duration;

public interface RateLimitService {
	public boolean checkLimit(String key, int limit, Duration duration);
	public long getRemainingRequests(String key, int limit);
	
}
