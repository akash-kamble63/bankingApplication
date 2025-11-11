package com.user_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Data;

@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "app.cache")
@Data
public class CacheConfig {
	 private boolean enabled = true;
	    private Ttl ttl = new Ttl();
	    
	    @Data
	    public static class Ttl {
	        private long userProfile = 300;      // 5 minutes
	        private long userList = 60;          // 1 minute
	        private long statistics = 600;       // 10 minutes
	        private long notifications = 300;    // 5 minutes
	        private long passwordResetToken = 3600;
	    }
	    
	    @Bean
	    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
	        // Configure ObjectMapper for cache serialization
	        ObjectMapper objectMapper = new ObjectMapper();
	        objectMapper.registerModule(new JavaTimeModule());
	        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	        
	        GenericJackson2JsonRedisSerializer serializer = 
	            new GenericJackson2JsonRedisSerializer(objectMapper);
	        
	        // Default cache configuration
	        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
	            .defaultCacheConfig()
	            .entryTtl(Duration.ofMinutes(5))
	            .serializeKeysWith(
	                RedisSerializationContext.SerializationPair.fromSerializer(
	                    new StringRedisSerializer()))
	            .serializeValuesWith(
	                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
	            .disableCachingNullValues();
	        
	        // Custom configurations for different caches
	        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
	            .builder(connectionFactory)
	            .cacheDefaults(defaultConfig);
	        
	        // User profile cache (5 minutes)
	        builder.withCacheConfiguration("userProfile",
	            RedisCacheConfiguration.defaultCacheConfig()
	                .entryTtl(Duration.ofSeconds(ttl.getUserProfile()))
	                .serializeKeysWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(
	                        new StringRedisSerializer()))
	                .serializeValuesWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(serializer)));
	        
	        // User list cache (1 minute)
	        builder.withCacheConfiguration("userList",
	            RedisCacheConfiguration.defaultCacheConfig()
	                .entryTtl(Duration.ofSeconds(ttl.getUserList()))
	                .serializeKeysWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(
	                        new StringRedisSerializer()))
	                .serializeValuesWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(serializer)));
	        
	        // Statistics cache (10 minutes)
	        builder.withCacheConfiguration("statistics",
	            RedisCacheConfiguration.defaultCacheConfig()
	                .entryTtl(Duration.ofSeconds(ttl.getStatistics()))
	                .serializeKeysWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(
	                        new StringRedisSerializer()))
	                .serializeValuesWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(serializer)));
	        
	        // Notifications cache (5 minutes)
	        builder.withCacheConfiguration("notifications",
	            RedisCacheConfiguration.defaultCacheConfig()
	                .entryTtl(Duration.ofSeconds(ttl.getNotifications()))
	                .serializeKeysWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(
	                        new StringRedisSerializer()))
	                .serializeValuesWith(
	                    RedisSerializationContext.SerializationPair.fromSerializer(serializer)));
	        
	        return builder.build();
	    }
}
