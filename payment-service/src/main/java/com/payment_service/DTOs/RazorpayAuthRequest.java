package com.payment_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayAuthRequest {
    private String token;
    private Long amount;
    private String currency;
    private String receipt;
    @Builder.Default
    private Boolean capture = false; // false for authorization only

    private String description;
    private String notes;
}
