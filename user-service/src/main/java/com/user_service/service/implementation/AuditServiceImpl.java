package com.user_service.service.implementation;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.enums.AuditAction;
import com.user_service.model.AuditLog;
import com.user_service.repository.AuditLogRepository;
import com.user_service.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Log audit event asynchronously
     */
    @Transactional
    @Async("auditExecutor")
    public void logAudit(AuditAction action, Long userId, String entityType, 
                        String entityId, Object details, String status) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            String detailsJson = details != null 
                ? objectMapper.writeValueAsString(details) 
                : null;
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(detailsJson)
                .ipAddress(request != null ? getClientIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status(status)
                .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created: action={}, userId={}", action, userId);
            
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log successful action
     */
    public void logSuccess(AuditAction action, Long userId, String entityType, 
                          String entityId, Object details) {
        logAudit(action, userId, entityType, entityId, details, "SUCCESS");
    }
    
    /**
     * Log failed action
     */
    public void logFailure(AuditAction action, Long userId, String entityType, 
                          String entityId, Object details, String errorMessage) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            String detailsJson = details != null 
                ? objectMapper.writeValueAsString(details) 
                : null;
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(detailsJson)
                .ipAddress(request != null ? getClientIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status("FAILURE")
                .errorMessage(errorMessage)
                .build();
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get audit logs for user
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Get audit logs by action
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
    
    /**
     * Get audit logs by date range
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }
    
    /**
     * Clean up old audit logs
     */
    @Transactional
    public void cleanupOldLogs(int retentionDays) {
        LocalDateTime retentionDate = LocalDateTime.now().minusDays(retentionDays);
        auditLogRepository.deleteOldLogs(retentionDate);
        log.info("Cleaned up audit logs older than {} days", retentionDays);
    }
    
    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * Get client IP address
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