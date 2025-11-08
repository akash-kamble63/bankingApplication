package com.account_service.patterns;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {
	private String eventId;
    private String aggregateId;
    private Long version;
    private LocalDateTime timestamp;
    private Long userId;

}
