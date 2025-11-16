package com.notification.DTOs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateNotificationRequest {
	private Long userId;
	private String referenceId;
	private String templateCode;
	private Map<String, Object> variables;
	private String correlationId;
}
