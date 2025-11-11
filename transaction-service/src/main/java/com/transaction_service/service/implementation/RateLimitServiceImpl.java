package com.transaction_service.service.implementation;


import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.transaction_service.service.RateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService{

    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Check if action is within rate limit
     * @param key Unique key for rate limiting (e.g., "transfer:userId:123")
     * @param limit Maximum number of requests
     * @param duration Time window
     * @return true if within limit, false if exceeded
     */
    public boolean checkLimit(String key, int limit, Duration duration) {
        try {
            String redisKey = "rate_limit:" + key;
            
            Long current = redisTemplate.opsForValue().increment(redisKey);
            
            if (current == null) {
                return false;
            }
            
            if (current == 1) {
                redisTemplate.expire(redisKey, duration);
            }
            
            boolean allowed = current <= limit;
            
            if (!allowed) {
                log.warn("Rate limit exceeded: key={}, current={}, limit={}", 
                    key, current, limit);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("Rate limit check failed: {}", e.getMessage());
            return true; // Fail open
        }
    }
    
    public long getRemainingRequests(String key, int limit) {
        String redisKey = "rate_limit:" + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        
        if (value == null) {
            return limit;
        }
        
        long current = Long.parseLong(value);
        return Math.max(0, limit - current);
    }
}
