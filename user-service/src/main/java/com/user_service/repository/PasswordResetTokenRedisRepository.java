package com.user_service.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.user_service.model.PasswordResetTokenRedis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRedisRepository {
    private final RedisTemplate<String, PasswordResetTokenRedis> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "password_reset_token:";
    private static final String USER_KEY_PREFIX = "user_reset_token:";

    /**
     * Save token with TTL
     */
    @SuppressWarnings("null")
    public void save(PasswordResetTokenRedis token, Duration ttl) {
        String tokenKey = KEY_PREFIX + token.getTokenHash();
        String userKey = USER_KEY_PREFIX + token.getUserId();

        redisTemplate.opsForValue().set(
                tokenKey, token, ttl.getSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(
                userKey, token.getTokenHash(), ttl.getSeconds(), TimeUnit.SECONDS);

        log.info("Saved password reset token with TTL: {} seconds", ttl.getSeconds());
    }

    /**
     * Find token by token string
     */
    public Optional<PasswordResetTokenRedis> findByTokenHash(String tokenHash) {
        String tokenKey = KEY_PREFIX + tokenHash;
        PasswordResetTokenRedis token = redisTemplate.opsForValue().get(tokenKey);
        return Optional.ofNullable(token);
    }

    /**
     * Find token by user ID
     */
    public Optional<PasswordResetTokenRedis> findByUserId(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        String tokenHash = stringRedisTemplate.opsForValue().get(userKey);

        if (tokenHash != null) {
            return findByTokenHash(tokenHash);
        }
        return Optional.empty();
    }

    /**
     * Delete token
     */
    public void delete(String token) {
        Optional<PasswordResetTokenRedis> tokenOpt = findByTokenHash(token);
        if (tokenOpt.isPresent()) {
            Long userId = tokenOpt.get().getUserId();
            redisTemplate.delete(KEY_PREFIX + token);
            stringRedisTemplate.delete(USER_KEY_PREFIX + userId);

            log.info("Deleted password reset token for user: {}", userId);
        }
    }

    /**
     * Delete all tokens for a user
     */
    public void deleteByUserId(Long userId) {
        Optional<PasswordResetTokenRedis> tokenOpt = findByUserId(userId);
        if (tokenOpt.isPresent()) {
            delete(tokenOpt.get().getTokenHash());
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
     * Get TTL in seconds
     */
    public Long getTTL(String token) {
        String key = KEY_PREFIX + token;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * Update token (mark as used)
     */
    public void markAsUsed(String tokenHash) {
        Optional<PasswordResetTokenRedis> tokenOpt = findByTokenHash(tokenHash);
        if (tokenOpt.isPresent()) {
            PasswordResetTokenRedis token = tokenOpt.get();
            token.setUsed(true);
            token.setUsedAt(java.time.LocalDateTime.now());
            // Keep TTL when updating
            Long remainingTTL = getTTL(tokenHash);
            if (remainingTTL != null && remainingTTL > 0) {
                save(token, Duration.ofSeconds(remainingTTL));
            }
        }
    }
}
