package com.notification.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {
	private Boolean emailEnabled;
	private Boolean smsEnabled;
	private Boolean pushEnabled;
	private Boolean inAppEnabled;
	private Boolean transactionAlerts;
	private Boolean paymentAlerts;
	private Boolean securityAlerts;
	private Boolean marketingAlerts;
	private Boolean promotionalAlerts;
	private Boolean quietHoursEnabled;
	private String quietHoursStart;
	private String quietHoursEnd;
}
