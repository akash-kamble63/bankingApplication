package com.payment_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayAuthorizationResponse {
	private boolean success;
    private String gatewayPaymentId;
    private String authorizationCode;
    private String errorCode;
    private String errorMessage;
    private String status;
}
