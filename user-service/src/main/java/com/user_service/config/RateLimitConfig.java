package com.user_service.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Data
public class RateLimitConfig {
	private boolean enabled = true;
    private int defaultLimit = 100; // requests per minute
    private Map<String, Integer> endpoints = new HashMap<>();

    /**
     * Create a bucket for rate limiting
     */
    public Bucket createBucket(int limit) {
        Bandwidth bandwidth = Bandwidth.builder()
            .capacity(limit)
            .refillIntervally(limit, Duration.ofMinutes(1))
            .build();
        
        return Bucket.builder()
            .addLimit(bandwidth)
            .build();
    }

    /**
     * Get limit for specific endpoint
     */
    public int getEndpointLimit(String endpoint) {
        return endpoints.getOrDefault(endpoint, defaultLimit);
    }
}
