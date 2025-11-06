package com.user_service.service;

import java.util.Map;

public interface CacheStatisticsService {
	Map<String, Object> getAllCacheStatistics();
    Map<String, Object> getCacheStatistics(String cacheName);

}
