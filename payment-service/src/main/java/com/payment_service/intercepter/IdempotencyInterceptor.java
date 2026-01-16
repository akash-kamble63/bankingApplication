package com.payment_service.intercepter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.entity.IdempotencyKey;
import com.payment_service.enums.IdempotencyStatus;
import com.payment_service.exception.IdempotencyConflictException;
import com.payment_service.exception.IdempotencyException;
import com.payment_service.service.IdempotencyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor to handle idempotency for payment endpoints.
 * Checks for Idempotency-Key header and manages request deduplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final int MAX_WAIT_TIME_MS = 30000; // 30 seconds
    private static final int POLL_INTERVAL_MS = 500;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Only apply to payment endpoints with POST method
        if (!shouldApplyIdempotency(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // Idempotency key is required for payment operations
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            sendError(response, HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required for payment operations");
            return;
        }

        // Validate idempotency key format (UUID or similar)
        if (!isValidIdempotencyKey(idempotencyKey)) {
            sendError(response, HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key format. Use UUID or similar unique identifier.");
            return;
        }

        Long userId = extractUserId();
        if (userId == null) {
            sendError(response, HttpStatus.UNAUTHORIZED, "User not authenticated");
            return;
        }

        // Wrap request and response for body caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            // Get or create idempotency key
            Object requestBody = parseRequestBody(wrappedRequest);

            IdempotencyKey key = idempotencyService.createOrGet(
                    idempotencyKey,
                    userId,
                    request.getRequestURI(),
                    request.getMethod(),
                    requestBody,
                    extractIpAddress(request),
                    request.getHeader("User-Agent"));

            // Handle based on status
            if (key.getStatus() == IdempotencyStatus.COMPLETED) {
                // Return cached response
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                sendCachedResponse(wrappedResponse, key);
                return;
            }

            if (key.getStatus() == IdempotencyStatus.PROCESSING) {
                // Wait for concurrent request to complete
                log.info("Request already processing, waiting: {}", idempotencyKey);
                try {
                    boolean completed = waitForCompletion(idempotencyKey);

                    if (completed) {
                        // Retrieve and return cached response
                        IdempotencyKey completedKey = idempotencyService
                                .createOrGet(idempotencyKey, userId, request.getRequestURI(),
                                        request.getMethod(), requestBody,
                                        extractIpAddress(request), request.getHeader("User-Agent"));
                        sendCachedResponse(wrappedResponse, completedKey);
                        return;
                    } else {
                        sendError(wrappedResponse, HttpStatus.REQUEST_TIMEOUT,
                                "Request processing timeout. Please retry with a new idempotency key.");
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    log.error("Thread interrupted while waiting for request completion", e);
                    sendError(wrappedResponse, HttpStatus.INTERNAL_SERVER_ERROR,
                            "Request processing was interrupted");
                    return;
                }
            }

            // Process new request
            request.setAttribute("idempotencyKey", idempotencyKey);
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // Cache successful response
            if (wrappedResponse.getStatus() >= 200 && wrappedResponse.getStatus() < 300) {
                String responseBody = new String(
                        wrappedResponse.getContentAsByteArray(),
                        StandardCharsets.UTF_8);

                // Extract payment reference from response
                String paymentReference = extractPaymentReference(responseBody);

                idempotencyService.markCompleted(
                        idempotencyKey,
                        paymentReference,
                        wrappedResponse.getStatus(),
                        responseBody);
            } else {
                // Mark as failed
                String errorBody = new String(
                        wrappedResponse.getContentAsByteArray(),
                        StandardCharsets.UTF_8);
                idempotencyService.markFailed(
                        idempotencyKey,
                        errorBody,
                        wrappedResponse.getStatus());
            }

            wrappedResponse.copyBodyToResponse();

        } catch (IdempotencyConflictException e) {
            log.error("Idempotency conflict: {}", e.getMessage());
            sendError(wrappedResponse, HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        } catch (IdempotencyException e) {
            log.error("Idempotency error: {}", e.getMessage());
            sendError(wrappedResponse, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in idempotency interceptor", e);
            idempotencyService.markFailed(idempotencyKey,
                    "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw e;
        }
    }

    private boolean shouldApplyIdempotency(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Apply to POST payment endpoints
        return "POST".equalsIgnoreCase(method) &&
                (path.contains("/api/v1/payments/card") ||
                        path.contains("/api/v1/payments/upi") ||
                        path.contains("/api/v1/payments/bill"));
    }

    private boolean isValidIdempotencyKey(String key) {
        // Must be 20-100 characters, alphanumeric with dashes and underscores
        return key != null &&
                key.length() >= 20 &&
                key.length() <= 100 &&
                key.matches("^[a-zA-Z0-9_-]+$");
    }

    private Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            try {
                return Long.parseLong(auth.getName());
            } catch (NumberFormatException e) {
                log.error("Invalid user ID format: {}", auth.getName());
            }
        }
        return null;
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private Object parseRequestBody(ContentCachingRequestWrapper request) throws IOException {
        byte[] body = request.getContentAsByteArray();
        if (body.length > 0) {
            return objectMapper.readValue(body, Object.class);
        }
        return null;
    }

    private boolean waitForCompletion(String idempotencyKey) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME_MS) {
            if (!idempotencyService.isProcessing(idempotencyKey)) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        return false;
    }

    private void sendCachedResponse(
            ContentCachingResponseWrapper response,
            IdempotencyKey key) throws IOException {

        response.setStatus(key.getResponseCode());
        response.setContentType("application/json");
        response.setHeader("X-Idempotency-Cached", "true");

        byte[] body = key.getResponseBody().getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(body);
        response.copyBodyToResponse();
    }

    private void sendError(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");

        String errorJson = String.format(
                "{\"error\": \"%s\", \"message\": \"%s\", \"status\": %d}",
                status.getReasonPhrase(),
                message,
                status.value());

        response.getWriter().write(errorJson);
    }

    private String extractPaymentReference(String responseBody) {
        try {
            var jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.has("paymentReference")) {
                return jsonNode.get("paymentReference").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract payment reference from response", e);
        }
        return null;
    }
}