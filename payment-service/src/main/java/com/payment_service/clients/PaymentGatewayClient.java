package com.payment_service.clients;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.payment_service.DTOs.BillPaymentResponse;
import com.payment_service.DTOs.BillerValidationResponse;
import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.DTOs.UpiStatusResponse;
import com.payment_service.DTOs.UpiTransactionResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor

public class PaymentGatewayClient {
	private final RazorpayGatewayClient razorpayClient;
	private final StripeGatewayClient stripeClient;

	@CircuitBreaker(name = "payment-gateway")
	public GatewayAuthorizationResponse authorizePayment(String gatewayName, String cardToken, BigDecimal amount,
			String currency, String reference) {
		return switch (gatewayName.toUpperCase()) {
		case "RAZORPAY" -> razorpayClient.authorize(cardToken, amount, currency, reference);
		case "STRIPE" -> stripeClient.authorize(cardToken, amount, currency, reference);
		default -> throw new IllegalArgumentException("Unsupported gateway: " + gatewayName);
		};
	}

	@CircuitBreaker(name = "payment-gateway")
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

	public UpiTransactionResponse initiateUpiTransaction(String upiId, BigDecimal amount, String reference) {
		// Integrate with UPI gateway (NPCI/PhonePe/Paytm)
		log.debug("Initiating UPI transaction: {}", reference);

		return UpiTransactionResponse.builder().success(true).transactionId("UPI-" + System.currentTimeMillis())
				.build();
	}

	public UpiStatusResponse checkUpiStatus(String transactionId) {
		// Poll UPI gateway for transaction status
		log.debug("Checking UPI status: {}", transactionId);

		return UpiStatusResponse.builder().transactionId(transactionId).status("COMPLETED").completed(true)
				.failed(false).build();
	}

	public BillerValidationResponse validateBiller(String billerId, String billNumber) {
		log.debug("Validating biller: {} - {}", billerId, billNumber);

		return BillerValidationResponse.builder().valid(true).billerName("Electricity Board").build();
	}

	public BillPaymentResponse payBill(String billerId, String billNumber, BigDecimal amount, String reference) {
		log.debug("Paying bill: {} - {} amount: {}", billerId, billNumber, amount);

		return BillPaymentResponse.builder().success(true).transactionId("BILL-" + System.currentTimeMillis()).build();
	}
}
