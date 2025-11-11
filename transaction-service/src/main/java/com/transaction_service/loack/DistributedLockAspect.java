package com.transaction_service.loack;
import java.time.Duration;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.transaction_service.annotation.DistributedLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
private final RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(distributedLock)")
    public Object acquireLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) 
            throws Throwable {
        
        String lockKey = generateLockKey(joinPoint, distributedLock);
        String lockValue = UUID.randomUUID().toString();
        
        boolean acquired = Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(
                lockKey, 
                lockValue, 
                Duration.ofSeconds(distributedLock.leaseTime())
            )
        );
        
        if (!acquired) {
            log.warn("Failed to acquire lock: {}", lockKey);
            throw new LockAcquisitionException("Could not acquire lock for: " + lockKey);
        }
        
        try {
            log.debug("Lock acquired: {}", lockKey);
            return joinPoint.proceed();
            
        } finally {
            // ✅ Only delete if we still own the lock
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
                log.debug("Lock released: {}", lockKey);
            }
        }
    }
    
    private String generateLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String key = distributedLock.key();
        
        if (key.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            key = signature.getMethod().getName();
        }
        
        // Parse SpEL expression for dynamic keys
        // Example: "account:#{#accountId}"
        Object[] args = joinPoint.getArgs();
        if (key.contains("#{")) {
            // Simple SpEL parsing (use SpelExpressionParser for complex cases)
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            
            for (int i = 0; i < paramNames.length; i++) {
                key = key.replace("#{#" + paramNames[i] + "}", String.valueOf(args[i]));
            }
        }
        
        return "lock:transaction:" + key;
    }
}
