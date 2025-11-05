package com.user_service.service;

public interface RateLimitService {
	boolean isAllowed(String key, int limit);
	boolean isAllowedForUser(String userId, String endpoint);
	boolean isAllowedForIp(String ipAddress, String endpoint);
	long getRemainingTokens(String key, int limit);
	void resetRateLimit(String key);
}
