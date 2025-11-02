package com.user_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakWebhookEvent {
	private String type;
    private String realmId;
    private String userId;
    private String clientId;
    private String ipAddress;
    
    @JsonProperty("time")
    private Long timestamp;
    
    private String error;
    private Object details;
}
