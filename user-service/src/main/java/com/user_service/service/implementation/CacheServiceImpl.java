package com.user_service.service.implementation;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.cache.Cache;
import com.user_service.service.CacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
	
private final CacheManager cacheManager;
    
    /**
     * Evict cache by name and key
     */
    public void evictCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Evicted cache: {} with key: {}", cacheName, key);
        }
    }
    
    /**
     * Evict all entries in a cache
     */
    public void evictAllCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared all entries in cache: {}", cacheName);
        }
    }
    
    /**
     * Evict user-specific caches
     */
    public void evictUserCaches(Long userId) {
        evictCache("userProfile", String.valueOf(userId));
        evictCache("statistics", "user:" + userId);
        evictCache("notifications", "user:" + userId);
        log.info("Evicted all caches for user: {}", userId);
    }
    
    /**
     * Evict user list caches
     */
    public void evictUserListCaches() {
        evictAllCache("userList");
        log.info("Evicted user list caches");
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            return String.format("Cache '%s' exists", cacheName);
        }
        return String.format("Cache '%s' not found", cacheName);
    }

}
