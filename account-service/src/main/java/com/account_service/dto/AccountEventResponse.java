package com.account_service.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountEventResponse {
    private Long id;
    private String eventId;
    private String aggregateId;
    private String eventType;
    private Long version;
    private String eventData;
    private String metadata;
    private LocalDateTime timestamp;
}