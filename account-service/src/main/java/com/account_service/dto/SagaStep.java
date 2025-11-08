package com.account_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SagaStep {
	private String stepName;
	private String serviceName;
	private String action;
	private String compensationAction;
	private Object data;
	private boolean completed;
	private boolean compensated;
}
