package com.user_service.service;

public interface CacheService {
	void evictCache(String cacheName, String key);
	void evictAllCache(String cacheName);
	void evictUserCaches(Long userId);
	void evictUserListCaches();
	String getCacheStats(String cacheName);
}
