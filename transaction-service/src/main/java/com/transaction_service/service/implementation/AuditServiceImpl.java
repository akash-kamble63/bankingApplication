package com.transaction_service.service.implementation;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction_service.entity.AuditLog;
import com.transaction_service.enums.AuditAction;
import com.transaction_service.repository.AuditLogRepository;
import com.transaction_service.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

	private final AuditLogRepository auditLogRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	@Async("auditExecutor")
	public void logSuccess(AuditAction action, Long userId, String entityType, String entityId, Object details) {
		logAudit(action, userId, entityType, entityId, details, "SUCCESS", null);
	}

	@Override
	@Transactional
	@Async("auditExecutor")
	public void logFailure(AuditAction action, Long userId, String entityType, String entityId, Object details,
			String errorMessage) {
		logAudit(action, userId, entityType, entityId, details, "FAILURE", errorMessage);
	}

	private void logAudit(AuditAction action, Long userId, String entityType, String entityId, Object details,
			String status, String errorMessage) {
		try {
			HttpServletRequest request = getCurrentRequest();
			String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;

			AuditLog auditLog = AuditLog.builder().userId(userId).action(action).entityType(entityType)
					.entityId(entityId).details(detailsJson).ipAddress(request != null ? getClientIp(request) : null)
					.userAgent(request != null ? request.getHeader("User-Agent") : null).status(status)
					.errorMessage(errorMessage).build();

			auditLogRepository.save(auditLog);

		} catch (Exception e) {
			log.error("Failed to create audit log: {}", e.getMessage(), e);
		}
	}

	private HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return attributes != null ? attributes.getRequest() : null;
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
