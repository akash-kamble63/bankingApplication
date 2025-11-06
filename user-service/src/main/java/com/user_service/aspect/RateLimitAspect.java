package com.user_service.aspect;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.user_service.annotation.RateLimit;
import com.user_service.dto.ApiResponse;
import com.user_service.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        
        HttpServletRequest request = ((ServletRequestAttributes) 
            RequestContextHolder.currentRequestAttributes()).getRequest();
        
        String ipAddress = getClientIp(request);
        String key = rateLimit.key().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : rateLimit.key();
        
        String rateLimitKey = String.format("rate_limit:ip:%s:%s", ipAddress, key);
        
        // Get remaining tokens before consuming
        long remainingBefore = rateLimitService.getRemainingTokens(rateLimitKey, rateLimit.limit());
        
        if (!rateLimitService.isAllowed(rateLimitKey, rateLimit.limit())) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ipAddress, key);
            
            HttpServletResponse response = ((ServletRequestAttributes) 
                    RequestContextHolder.currentRequestAttributes()).getResponse();
            
            if (response != null) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 60));
                response.setHeader("Retry-After", "60");
            }
            
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("429", "Too many requests. Please try again later."));
        }
        
        // Add rate limit headers to successful responses
        HttpServletResponse response = ((ServletRequestAttributes) 
                RequestContextHolder.currentRequestAttributes()).getResponse();
        
        if (response != null) {
            long remainingAfter = rateLimitService.getRemainingTokens(rateLimitKey, rateLimit.limit());
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remainingAfter)));
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * Get client IP address (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}