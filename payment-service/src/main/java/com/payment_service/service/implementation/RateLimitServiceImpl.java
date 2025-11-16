package com.payment_service.service.implementation;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.payment_service.service.RateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService{
private final RedisTemplate<String, String> redisTemplate;
    
    private static final int MAX_PAYMENTS_PER_HOUR = 50;
    private static final int MAX_PAYMENTS_PER_DAY = 200;

    public boolean checkPaymentLimit(Long userId) {
        String hourlyKey = "rate:payment:hourly:" + userId;
        String dailyKey = "rate:payment:daily:" + userId;
        
        // Check hourly limit
        Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey);
        if (hourlyCount == 1) {
            redisTemplate.expire(hourlyKey, Duration.ofHours(1));
        }
        
        if (hourlyCount > MAX_PAYMENTS_PER_HOUR) {
            log.warn("Hourly payment limit exceeded for user: {}", userId);
            return false;
        }
        
        // Check daily limit
        Long dailyCount = redisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount == 1) {
            redisTemplate.expire(dailyKey, Duration.ofDays(1));
        }
        
        if (dailyCount > MAX_PAYMENTS_PER_DAY) {
            log.warn("Daily payment limit exceeded for user: {}", userId);
            return false;
        }
        
        return true;
    }

    public void recordPaymentAttempt(Long userId, boolean success) {
        String key = "payment:attempts:" + userId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }
}
