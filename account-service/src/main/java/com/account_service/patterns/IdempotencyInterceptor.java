package com.account_service.patterns;

import java.util.Optional;

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
	private final IdempotencyService idempotencyService;
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if (!(handler instanceof HandlerMethod)) {
			return true;
		}

		HandlerMethod handlerMethod = (HandlerMethod) handler;
		Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);

		if (idempotent == null) {
			return true; // Not an idempotent endpoint
		}

		String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

		if (idempotencyKey == null || idempotencyKey.isEmpty()) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			response.getWriter().write("{\"error\":\"Idempotency-Key header required\"}");
			return false;
		}

		// Check if request already processed
		Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);

		if (existingRecord.isPresent()) {
			IdempotencyRecord record = existingRecord.get();

			if (record.getProcessing()) {
				// Request is being processed
				response.setStatus(HttpStatus.CONFLICT.value());
				response.getWriter().write("{\"error\":\"Request already processing\"}");
				return false;
			}

			if (idempotent.returnCached() && record.getResponseBody() != null) {
				// Return cached response
				response.setStatus(record.getResponseStatus());
				response.setContentType("application/json");
				response.getWriter().write(record.getResponseBody());
				log.info("Returned cached response for idempotency key: {}", idempotencyKey);
				return false;
			}
		}

		// Store idempotency key in request for later use
		request.setAttribute("idempotencyKey", idempotencyKey);
		return true;
	}
}
