package com.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.service.CacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheController {

	 private final CacheService cacheService;
	    
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
	    
	    /**
	     * Get cache stats
	     */
	    @GetMapping("/{cacheName}/stats")
	    @PreAuthorize("hasRole('ADMIN')")
	    public ResponseEntity<ApiResponse<String>> getCacheStats(@PathVariable String cacheName) {
	        String stats = cacheService.getCacheStats(cacheName);
	        return ResponseEntity.ok(ApiResponse.success(stats));
	    }
}
