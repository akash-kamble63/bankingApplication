package com.user_service.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.user_service.annotation.Auditable;
import com.user_service.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

	private final AuditService auditService;

	@AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
	public void auditSuccess(JoinPoint joinPoint, Auditable auditable, Object result) {
	    try {
	        Long userId = getCurrentUserId();
	        String entityId = extractEntityId(joinPoint);
	        
	        // Call async audit service
	        auditService.logSuccess(
	            auditable.action(), 
	            userId, 
	            auditable.entityType(), 
	            entityId,
	            result
	        );
	    } catch (Exception e) {
	        log.error("Failed to create audit log: {}", e.getMessage(), e);
	    }
	}

	@AfterThrowing(pointcut = "@annotation(auditable)", throwing = "error")
	public void auditFailure(JoinPoint joinPoint, Auditable auditable, Throwable error) {
		try {
			Long userId = getCurrentUserId();
			Object[] args = joinPoint.getArgs();

			auditService.logFailure(auditable.action(), userId, auditable.entityType(), extractEntityId(joinPoint),
					args, error.getMessage());

		} catch (Exception e) {
			log.error("Failed to create audit log for failure: {}", e.getMessage(), e);
		}
	}

	/**
	 * Extract user ID from JWT token (Keycloak)
	 */
	private Long getCurrentUserId() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();

			if (auth == null || !auth.isAuthenticated()) {
				return null;
			}

			// For JWT tokens from Keycloak
			if (auth instanceof JwtAuthenticationToken jwtAuth) {
				Jwt jwt = jwtAuth.getToken();

				// Try to get user ID from different possible claims
				String userId = jwt.getClaimAsString("sub"); // Subject (user ID)
				if (userId == null) {
					userId = jwt.getClaimAsString("user_id");
				}
				if (userId == null) {
					userId = jwt.getClaimAsString("preferred_username");
				}

				if (userId != null) {
					try {
						return Long.parseLong(userId);
					} catch (NumberFormatException e) {
						// If it's a UUID or string ID, you might need to hash it or handle differently
						log.warn("User ID is not a Long: {}", userId);
						return userId.hashCode() & 0xFFFFFFFFL; // Convert to positive long
					}
				}
			}

			// Fallback: try to parse the principal name
			String principalName = auth.getName();
			if (principalName != null) {
				try {
					return Long.parseLong(principalName);
				} catch (NumberFormatException e) {
					log.debug("Cannot parse principal name as Long: {}", principalName);
				}
			}

		} catch (Exception e) {
			log.error("Error extracting user ID: {}", e.getMessage());
		}

		return null;
	}

	/**
	 * Extract entity ID from method parameters Looks for parameters named 'id',
	 * 'userId', or annotated with @PathVariable
	 */
	private String extractEntityId(JoinPoint joinPoint) {
		try {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			Parameter[] parameters = method.getParameters();
			Object[] args = joinPoint.getArgs();

			// Look for common ID parameter names
			for (int i = 0; i < parameters.length; i++) {
				String paramName = parameters[i].getName();
				if (args[i] != null && (paramName.equalsIgnoreCase("id") || paramName.equalsIgnoreCase("userId")
						|| paramName.equalsIgnoreCase("entityId"))) {
					return args[i].toString();
				}
			}

			// Fallback: if first parameter looks like an ID
			if (args.length > 0 && args[0] != null) {
				Class<?> firstParamType = args[0].getClass();
				if (Number.class.isAssignableFrom(firstParamType) || firstParamType == String.class) {
					return args[0].toString();
				}
			}

		} catch (Exception e) {
			log.debug("Could not extract entity ID: {}", e.getMessage());
		}

		return null;
	}

	/**
	 * Get client IP address from request
	 */
	private String getClientIpAddress() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
					.getRequestAttributes();

			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();

				String ip = request.getHeader("X-Forwarded-For");
				if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getHeader("X-Real-IP");
				}
				if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
					ip = request.getRemoteAddr();
				}

				// Handle multiple IPs in X-Forwarded-For
				if (ip != null && ip.contains(",")) {
					ip = ip.split(",")[0].trim();
				}

				return ip;
			}
		} catch (Exception e) {
			log.debug("Could not extract IP address: {}", e.getMessage());
		}

		return null;
	}
}