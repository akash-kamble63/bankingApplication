package com.payment_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayCaptureResponse {
	private String id;
	private String entity;
	private Long amount;
	private String currency;
	private String status;
	private String orderId;
	private String invoiceId;
	private Boolean international;
	private String method;
	private Long amountRefunded;
	private String refundStatus;
	private Boolean captured;
	private String description;
	private Long fee;
	private Long tax;
	private String errorCode;
	private String errorDescription;
	private Long createdAt;
}
