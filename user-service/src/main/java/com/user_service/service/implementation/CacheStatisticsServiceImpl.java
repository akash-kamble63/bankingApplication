package com.user_service.service.implementation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.user_service.service.CacheStatisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class CacheStatisticsServiceImpl implements CacheStatisticsService {
	private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Map<String, Object> getAllCacheStatistics() {
        Map<String, Object> allStats = new HashMap<>();
        
        // Get all cache names
        cacheManager.getCacheNames().forEach(cacheName -> {
            allStats.put(cacheName, getCacheStatistics(cacheName));
        });
        
        // Add global Redis stats
        allStats.put("redis", getRedisStatistics());
        
        return allStats;
    }
    
    @Override
    public Map<String, Object> getCacheStatistics(String cacheName) {
        Map<String, Object> stats = new HashMap<>();
        
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            stats.put("exists", false);
            return stats;
        }
        
        stats.put("exists", true);
        stats.put("name", cacheName);
        stats.put("type", cache.getClass().getSimpleName());
        
        // If it's a Redis cache, get detailed stats
        if (cache instanceof RedisCache) {
            RedisCache redisCache = (RedisCache) cache;
            
            try {
                // Get cache prefix
                String cachePrefix = cacheName + "::*";
                
                // Count keys in this cache
                Set<String> keys = redisTemplate.keys(cachePrefix);
                int keyCount = keys != null ? keys.size() : 0;
                
                stats.put("keyCount", keyCount);
                stats.put("keys", keys != null ? keys.stream().limit(10).toList() : null);
                stats.put("sampleSize", Math.min(10, keyCount));
                
            } catch (Exception e) {
                log.warn("Failed to get Redis cache statistics for {}: {}", 
                    cacheName, e.getMessage());
                stats.put("error", e.getMessage());
            }
        }
        
        return stats;
    }
    
    /**
     * Get Redis server statistics
     */
    private Map<String, Object> getRedisStatistics() {
        Map<String, Object> redisStats = new HashMap<>();
        
        try {
            // Check Redis connection
            redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            redisStats.put("connected", true);
            
            // Get database size
            Long dbSize = redisTemplate.getConnectionFactory()
                .getConnection()
                .dbSize();
            
            redisStats.put("totalKeys", dbSize);
            
            // Get all cache-related keys
            Set<String> allCacheKeys = redisTemplate.keys("*");
            redisStats.put("allKeysCount", allCacheKeys != null ? allCacheKeys.size() : 0);
            
        } catch (Exception e) {
            log.error("Failed to get Redis statistics: {}", e.getMessage());
            redisStats.put("connected", false);
            redisStats.put("error", e.getMessage());
        }
        
        return redisStats;
    }
}
