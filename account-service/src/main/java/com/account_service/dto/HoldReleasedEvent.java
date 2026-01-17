package com.account_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HoldReleasedEvent {
    private String accountNumber;
    private Long holdId;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime releasedAt;
}
