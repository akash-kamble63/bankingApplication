package com.user_service.aspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.user_service.annotation.Auditable;
import com.user_service.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    
    private final AuditService auditService;
    
    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result"
    )
    public void auditSuccess(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Long userId = getCurrentUserId();
            Object[] args = joinPoint.getArgs();
            
            auditService.logSuccess(
                auditable.action(),
                userId,
                auditable.entityType(),
                extractEntityId(args),
                result
            );
            
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }
    
    @AfterThrowing(
        pointcut = "@annotation(auditable)",
        throwing = "error"
    )
    public void auditFailure(JoinPoint joinPoint, Auditable auditable, Throwable error) {
        try {
            Long userId = getCurrentUserId();
            Object[] args = joinPoint.getArgs();
            
            auditService.logFailure(
                auditable.action(),
                userId,
                auditable.entityType(),
                extractEntityId(args),
                args,
                error.getMessage()
            );
            
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }
    
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // Extract user ID from JWT or authentication object
            // This is simplified - implement based on your auth structure
            return null; // Replace with actual user ID extraction
        }
        return null;
    }
    
    private String extractEntityId(Object[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }
}