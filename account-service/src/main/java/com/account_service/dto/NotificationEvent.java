package com.account_service.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long userId;
    private String type; // FUND_TRANSFER_FAILED, BENEFICIARY_VERIFICATION_FAILED, etc.
    private String title;
    private String message;
    private String priority; // LOW, MEDIUM, HIGH

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private Map<String, String> metadata;

    // Optional: Specify delivery channels
    @Builder.Default
    private boolean sendEmail = true;

    @Builder.Default
    private boolean sendPush = true;

    @Builder.Default
    private boolean sendSms = false;
}
