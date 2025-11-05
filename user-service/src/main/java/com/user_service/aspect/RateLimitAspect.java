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
        
        String ipAddress = request.getRemoteAddr();
        String key = rateLimit.key().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : rateLimit.key();
        
        String rateLimitKey = String.format("rate_limit:ip:%s:%s", ipAddress, key);
        
        if (!rateLimitService.isAllowed(rateLimitKey, rateLimit.limit())) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ipAddress, key);
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("429", "Too many requests. Please try again later."));
        }
        
        return joinPoint.proceed();
    }
}