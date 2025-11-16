package com.user_service.event;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor

public class UserEventPublisher {
	 private final KafkaTemplate<String, Object> kafkaTemplate;
	    private final ObjectMapper objectMapper;

	    /**
	     * ✅ FIXED: Publish User Registered Event
	     */
	    public void publishUserRegistered(User user) {
	        try {
	            Map<String, Object> event = new HashMap<>();
	            event.put("eventType", "UserRegistered");
	            event.put("aggregateId", "USER-" + user.getId());
	            event.put("timestamp", LocalDateTime.now().toString());
	            event.put("correlationId", UUID.randomUUID().toString());
	            
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("userId", user.getId());
	            payload.put("username", user.getUsername());
	            payload.put("email", user.getEmail());
	            payload.put("contactNo", user.getContactNo());
	            payload.put("status", user.getStatus().name());
	            payload.put("authId", user.getAuthId());
	            
	            // ✅ Get profile data if exists
	            if (user.getProfile() != null) {
	                payload.put("firstName", user.getProfile().getFirstName());
	                payload.put("lastName", user.getProfile().getLastName());
	                payload.put("gender", user.getProfile().getGender());
	                payload.put("address", user.getProfile().getAddress());
	                payload.put("occupation", user.getProfile().getOccupation());
	                payload.put("nationality", user.getProfile().getNationality());
	            }
	            
	            event.put("payload", payload);
	            
	            publishEvent("banking.user.events", "USER-" + user.getId(), event);
	            log.info("Published UserRegistered event for user: {}", user.getId());
	            
	        } catch (Exception e) {
	            log.error("Failed to publish UserRegistered event: {}", e.getMessage(), e);
	        }
	    }

	    /**
	     * ✅ FIXED: Publish User Profile Updated Event
	     */
	    public void publishUserProfileUpdated(User user, Map<String, Object> changes) {
	        try {
	            Map<String, Object> event = new HashMap<>();
	            event.put("eventType", "UserProfileUpdated");
	            event.put("aggregateId", "USER-" + user.getId());
	            event.put("timestamp", LocalDateTime.now().toString());
	            
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("userId", user.getId());
	            payload.put("email", user.getEmail());
	            payload.put("changes", changes);
	            
	            event.put("payload", payload);
	            
	            publishEvent("banking.user.events", "USER-" + user.getId(), event);
	            log.info("Published UserProfileUpdated event for user: {}", user.getId());
	            
	        } catch (Exception e) {
	            log.error("Failed to publish UserProfileUpdated event: {}", e.getMessage(), e);
	        }
	    }

	    /**
	     * ✅ NEW: Publish User Status Changed Event
	     */
	    public void publishUserStatusChanged(User user, String oldStatus, String newStatus, String reason) {
	        try {
	            Map<String, Object> event = new HashMap<>();
	            event.put("eventType", "UserStatusChanged");
	            event.put("aggregateId", "USER-" + user.getId());
	            event.put("timestamp", LocalDateTime.now().toString());
	            
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("userId", user.getId());
	            payload.put("email", user.getEmail());
	            payload.put("oldStatus", oldStatus);
	            payload.put("newStatus", newStatus);
	            payload.put("reason", reason != null ? reason : "N/A");
	            
	            event.put("payload", payload);
	            
	            publishEvent("banking.user.events", "USER-" + user.getId(), event);
	            log.info("Published UserStatusChanged event for user: {} ({} -> {})", 
	                user.getId(), oldStatus, newStatus);
	            
	        } catch (Exception e) {
	            log.error("Failed to publish UserStatusChanged event: {}", e.getMessage(), e);
	        }
	    }

	    /**
	     * ✅ FIXED: Publish User Login Event
	     */
	    public void publishUserLogin(Long userId, String email, String ipAddress, 
	                                 String userAgent, boolean success) {
	        try {
	            Map<String, Object> event = new HashMap<>();
	            event.put("eventType", success ? "UserLoginSuccess" : "UserLoginFailed");
	            event.put("aggregateId", "USER-" + userId);
	            event.put("timestamp", LocalDateTime.now().toString());
	            
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("userId", userId);
	            payload.put("email", email);
	            payload.put("ipAddress", ipAddress);
	            payload.put("userAgent", userAgent);
	            payload.put("success", success);
	            payload.put("loginTimestamp", System.currentTimeMillis());
	            
	            event.put("payload", payload);
	            
	            publishEvent("banking.user.events", "USER-" + userId, event);
	            
	        } catch (Exception e) {
	            log.error("Failed to publish UserLogin event: {}", e.getMessage(), e);
	        }
	    }

	    /**
	     * ✅ NEW: Publish Email Verified Event
	     */
	    public void publishEmailVerified(User user) {
	        try {
	            Map<String, Object> event = new HashMap<>();
	            event.put("eventType", "EmailVerified");
	            event.put("aggregateId", "USER-" + user.getId());
	            event.put("timestamp", LocalDateTime.now().toString());
	            
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("userId", user.getId());
	            payload.put("email", user.getEmail());
	            payload.put("verifiedAt", user.getEmailVerifiedAt() != null ? 
	                user.getEmailVerifiedAt().toString() : LocalDateTime.now().toString());
	            
	            event.put("payload", payload);
	            
	            publishEvent("banking.user.events", "USER-" + user.getId(), event);
	            log.info("Published EmailVerified event for user: {}", user.getId());
	            
	        } catch (Exception e) {
	            log.error("Failed to publish EmailVerified event: {}", e.getMessage(), e);
	        }
	    }

	    /**
	     * ✅ Publish User Notification Request
	     */
	    public void publishNotificationRequest(Long userId, String email, 
	                                          String notificationType, 
	                                          String channel, 
	                                          Map<String, Object> data) {
	        try {
	            Map<String, Object> event = new HashMap<>();
	            event.put("eventType", "NotificationRequested");
	            event.put("aggregateId", "USER-" + userId);
	            event.put("timestamp", LocalDateTime.now().toString());
	            
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("userId", userId);
	            payload.put("email", email);
	            payload.put("notificationType", notificationType);
	            payload.put("channel", channel);
	            payload.put("data", data);
	            
	            event.put("payload", payload);
	            
	            publishEvent("banking.user.notifications", "USER-" + userId, event);
	            
	        } catch (Exception e) {
	            log.error("Failed to publish notification request: {}", e.getMessage(), e);
	        }
	    }

	    /**
	     * Generic event publisher with error handling
	     */
	    private void publishEvent(String topic, String key, Map<String, Object> event) {
	        CompletableFuture<SendResult<String, Object>> future = 
	            kafkaTemplate.send(topic, key, event);
	        
	        future.whenComplete((result, ex) -> {
	            if (ex != null) {
	                log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
	            } else {
	                log.debug("Event sent successfully to topic {} partition {} offset {}", 
	                    topic,
	                    result.getRecordMetadata().partition(),
	                    result.getRecordMetadata().offset());
	            }
	        });
	    }
}
