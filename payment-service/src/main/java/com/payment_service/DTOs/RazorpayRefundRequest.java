package com.payment_service.DTOs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayRefundRequest {
    private Long amount; // Optional - omit for full refund
    private String notes;
    private String receipt;
}
