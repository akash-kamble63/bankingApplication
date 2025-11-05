package com.user_service.service.implementation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.user_service.config.RateLimitConfig;
import com.user_service.service.RateLimitService;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

	 private final RateLimitConfig rateLimitConfig;
	    private final RedisTemplate<String, Object> redisTemplate;
	    
	    // In-memory cache of buckets (can be moved to Redis for distributed systems)
	    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
	    
	    /**
	     * Check if request is allowed for given key
	     */
	    public boolean isAllowed(String key, int limit) {
	        if (!rateLimitConfig.isEnabled()) {
	            return true;
	        }
	        
	        Bucket bucket = buckets.computeIfAbsent(key, k -> 
	            rateLimitConfig.createBucket(limit)
	        );
	        
	        boolean allowed = bucket.tryConsume(1);
	        
	        if (!allowed) {
	            log.warn("Rate limit exceeded for key: {}", key);
	        }
	        
	        return allowed;
	    }
	    
	    /**
	     * Check if request is allowed for user
	     */
	    public boolean isAllowedForUser(String userId, String endpoint) {
	        String key = String.format("rate_limit:%s:%s", userId, endpoint);
	        int limit = rateLimitConfig.getEndpointLimit(endpoint);
	        return isAllowed(key, limit);
	    }
	    
	    /**
	     * Check if request is allowed for IP
	     */
	    public boolean isAllowedForIp(String ipAddress, String endpoint) {
	        String key = String.format("rate_limit:ip:%s:%s", ipAddress, endpoint);
	        int limit = rateLimitConfig.getEndpointLimit(endpoint);
	        return isAllowed(key, limit);
	    }
	    
	    /**
	     * Get remaining tokens for key
	     */
	    public long getRemainingTokens(String key, int limit) {
	        Bucket bucket = buckets.get(key);
	        if (bucket == null) {
	            return limit;
	        }
	        return bucket.getAvailableTokens();
	    }
	    
	    /**
	     * Reset rate limit for key (admin function)
	     */
	    public void resetRateLimit(String key) {
	        buckets.remove(key);
	        log.info("Rate limit reset for key: {}", key);
	    }

}
