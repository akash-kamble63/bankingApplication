package com.payment_service.service;

import java.math.BigDecimal;

import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;

public interface PaymentGatewayService {
	GatewayAuthorizationResponse authorizePayment(String gatewayName, String cardToken, BigDecimal amount,
			String currency, String reference);

	GatewayCaptureResponse capturePayment(String gatewayName, String gatewayPaymentId, BigDecimal amount);

	void refundPayment(String gatewayName, String gatewayPaymentId, BigDecimal amount);

	void voidAuthorization(String gatewayName, String gatewayPaymentId);

}
