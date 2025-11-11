package com.user_service.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

@Configuration
public class LockConfiguration {
	 /**
     * Create a Redis-based distributed lock registry
     * This ensures locks work across multiple application instances/pods
     * 
     * If you don't have Redis, use DefaultLockRegistry for single-instance deployment
     */
    @Bean
    public LockRegistry lockRegistry(RedisConnectionFactory redisConnectionFactory) {
        // For distributed/multi-instance deployment (RECOMMENDED)
        return new RedisLockRegistry(
                redisConnectionFactory, 
                "user-service-locks",  // Registry key prefix
                TimeUnit.SECONDS.toMillis(60)  // Lock expiration time (60 seconds)
        );
        
        // For single-instance deployment (NOT recommended for production):
        // return new DefaultLockRegistry();
    }
}
