package com.user_service.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.user_service.model.PasswordResetTokenRedis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRedisRepository {
private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String KEY_PREFIX = "password_reset:";
    private static final String USER_KEY_PREFIX = "user_reset:";
    
    /**
     * Save token with TTL
     */
    public void save(PasswordResetTokenRedis token, Duration ttl) {
        String key = KEY_PREFIX + token.getToken();
        String userKey = USER_KEY_PREFIX + token.getUserId();
        
        redisTemplate.opsForValue().set(key, token, ttl);
        redisTemplate.opsForValue().set(userKey, token.getToken(), ttl);
        
        log.info("Saved password reset token with TTL: {} seconds", ttl.getSeconds());
    }
    
    /**
     * Find token by token string
     */
    public Optional<PasswordResetTokenRedis> findByToken(String token) {
        String key = KEY_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof PasswordResetTokenRedis) {
            return Optional.of((PasswordResetTokenRedis) value);
        }
        
        return Optional.empty();
    }
    
    /**
     * Find token by user ID
     */
    public Optional<PasswordResetTokenRedis> findByUserId(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        Object tokenValue = redisTemplate.opsForValue().get(userKey);
        
        if (tokenValue instanceof String) {
            return findByToken((String) tokenValue);
        }
        
        return Optional.empty();
    }
    
    /**
     * Delete token
     */
    public void delete(String token) {
        Optional<PasswordResetTokenRedis> tokenOpt = findByToken(token);
        if (tokenOpt.isPresent()) {
            PasswordResetTokenRedis resetToken = tokenOpt.get();
            redisTemplate.delete(KEY_PREFIX + token);
            redisTemplate.delete(USER_KEY_PREFIX + resetToken.getUserId());
            log.info("Deleted password reset token for user: {}", resetToken.getUserId());
        }
    }
    
    /**
     * Delete all tokens for a user
     */
    public void deleteByUserId(Long userId) {
        Optional<PasswordResetTokenRedis> tokenOpt = findByUserId(userId);
        if (tokenOpt.isPresent()) {
            delete(tokenOpt.get().getToken());
        }
    }
    
    /**
     * Check if token exists
     */
    public boolean exists(String token) {
        String key = KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Get remaining TTL for token
     */
    public Long getTTL(String token) {
        String key = KEY_PREFIX + token;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
    
    /**
     * Update token (mark as used)
     */
    public void markAsUsed(String token) {
        Optional<PasswordResetTokenRedis> tokenOpt = findByToken(token);
        if (tokenOpt.isPresent()) {
            PasswordResetTokenRedis resetToken = tokenOpt.get();
            resetToken.setUsed(true);
            
            // Get remaining TTL and save with same TTL
            Long ttl = getTTL(token);
            if (ttl != null && ttl > 0) {
                save(resetToken, Duration.ofSeconds(ttl));
            }
        }
    }
}
