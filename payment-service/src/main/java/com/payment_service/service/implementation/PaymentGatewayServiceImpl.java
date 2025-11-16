package com.payment_service.service.implementation;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.clients.RazorpayGatewayClient;
import com.payment_service.clients.StripeGatewayClient;
import com.payment_service.service.PaymentGatewayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {
	private final RazorpayGatewayClient razorpayClient;
	private final StripeGatewayClient stripeClient;

	/**
	 * Route to appropriate gateway based on gateway name
	 */
	public GatewayAuthorizationResponse authorizePayment(String gatewayName, String cardToken, BigDecimal amount,
			String currency, String reference) {

		return switch (gatewayName.toUpperCase()) {
		case "RAZORPAY" -> razorpayClient.authorize(cardToken, amount, currency, reference);
		case "STRIPE" -> stripeClient.authorize(cardToken, amount, currency, reference);
		default -> throw new IllegalArgumentException("Unsupported gateway: " + gatewayName);
		};
	}

	public GatewayCaptureResponse capturePayment(String gatewayName, String gatewayPaymentId, BigDecimal amount) {

		return switch (gatewayName.toUpperCase()) {
		case "RAZORPAY" -> razorpayClient.capture(gatewayPaymentId, amount);
		case "STRIPE" -> stripeClient.capture(gatewayPaymentId, amount);

		default -> throw new IllegalArgumentException("Unsupported gateway");
		};
	}

	public void refundPayment(String gatewayName, String gatewayPaymentId, BigDecimal amount) {
		switch (gatewayName.toUpperCase()) {
		case "RAZORPAY" -> razorpayClient.refund(gatewayPaymentId, amount);
		case "STRIPE" -> stripeClient.refund(gatewayPaymentId, amount);

		}
	}

	public void voidAuthorization(String gatewayName, String gatewayPaymentId) {
		switch (gatewayName.toUpperCase()) {
		case "RAZORPAY" -> razorpayClient.voidAuth(gatewayPaymentId);
		case "STRIPE" -> stripeClient.voidAuth(gatewayPaymentId);

		}
	}
}
