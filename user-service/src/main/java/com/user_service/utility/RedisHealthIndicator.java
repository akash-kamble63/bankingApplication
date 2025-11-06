package com.user_service.utility;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {
	
	private final RedisTemplate<String, Object> redisTemplate;

	public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public Health health() {
		try {
			redisTemplate.getConnectionFactory().getConnection().ping();
			return Health.up().build();
		} catch (Exception e) {
			return Health.down(e).build();
		}
	}
}
