package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPreferencesRequest {
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean emailOnLogin;
    private Boolean emailOnPasswordChange;
    private Boolean emailOnProfileUpdate;
    private Boolean emailOnAccountActivity;
    private Boolean emailMarketing;
    private Boolean emailSecurityAlerts;
}
