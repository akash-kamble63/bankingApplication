package com.user_service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.service.CacheService;
import com.user_service.service.CacheStatisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheController {

	private final CacheService cacheService;
	private final CacheStatisticsService cacheStatisticsService;
	
	/**
     * Get all cache statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCacheStats() {
        log.info("Fetching all cache statistics");
        Map<String, Object> stats = cacheStatisticsService.getAllCacheStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Cache statistics retrieved"));
    }
    
    /**
     * Get specific cache statistics
     */
    @GetMapping("/{cacheName}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats(
            @PathVariable String cacheName) {
        log.info("Fetching cache statistics for: {}", cacheName);
        Map<String, Object> stats = cacheStatisticsService.getCacheStatistics(cacheName);
        return ResponseEntity.ok(ApiResponse.success(stats, "Cache statistics retrieved"));
    }
    
    /**
     * Clear specific cache
     */
    @DeleteMapping("/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearCache(@PathVariable String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        cacheService.evictAllCache(cacheName);
        return ResponseEntity.ok(ApiResponse.success("Cache cleared successfully"));
    }
    
    /**
     * Clear specific cache key
     */
    @DeleteMapping("/{cacheName}/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearCacheKey(
            @PathVariable String cacheName,
            @PathVariable String key) {
        log.info("Clearing cache: {} with key: {}", cacheName, key);
        cacheService.evictCache(cacheName, key);
        return ResponseEntity.ok(ApiResponse.success("Cache key cleared successfully"));
    }
    
    /**
     * Clear user-specific caches
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearUserCaches(@PathVariable Long userId) {
        log.info("Clearing all caches for user: {}", userId);
        cacheService.evictUserCaches(userId);
        return ResponseEntity.ok(ApiResponse.success("User caches cleared successfully"));
    }
	
}
