package com.account_service.patterns;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.account_service.annotation.Idempotent;
import com.account_service.model.IdempotencyRecord;
import com.account_service.service.IdempotencyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {
    private final RedisTemplate<String, Object> redisTemplate;
    private final IdempotencyService idempotencyService;
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String LOCK_PREFIX = "idempotency_lock:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod))
            return true;

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null)
            return true;

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("{\"error\":\"Idempotency-Key header required\"}");
            return false;
        }

        // Check if already processed
        Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
        if (existingRecord.isPresent() && !existingRecord.get().getProcessing()) {
            // Return cached response
            IdempotencyRecord record = existingRecord.get();
            response.setStatus(record.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(record.getResponseBody());
            return false;
        }

        // Atomic lock acquisition with SETNX
        String lockKey = LOCK_PREFIX + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "processing", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            // Another request is processing
            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write("{\"error\":\"Request already processing\"}");
            return false;
        }

        // Store lock key for cleanup in afterCompletion
        request.setAttribute("idempotencyLock", lockKey);
        request.setAttribute("idempotencyKey", idempotencyKey);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        // release lock
        String lockKey = (String) request.getAttribute("idempotencyLock");
        if (lockKey != null) {
            redisTemplate.delete(lockKey);
        }
    }
}
