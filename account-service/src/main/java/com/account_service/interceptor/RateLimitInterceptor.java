package com.account_service.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter beneficiaryRateLimiter;
    private final RateLimiter searchRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String requestURI = request.getRequestURI();
        String userId = request.getHeader("X-User-Id"); // Assuming user ID in header

        if (userId == null) {
            userId = "anonymous";
        }

        try {
            // Apply rate limiting based on endpoint
            if (requestURI.contains("/beneficiaries/search")) {
                // More restrictive for search
                searchRateLimiter.acquirePermission();
                log.debug("Search rate limit check passed for user: {}", userId);

            } else if (requestURI.contains("/beneficiaries")) {
                // General beneficiary operations
                beneficiaryRateLimiter.acquirePermission();
                log.debug("Beneficiary rate limit check passed for user: {}", userId);
            }

            return true;

        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for user {} on endpoint {}", userId, requestURI);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Rate limit exceeded\", " +
                            "\"message\": \"Too many requests. Please try again later.\", " +
                            "\"retryAfter\": \"60\"}");

            return false;
        }
    }
}