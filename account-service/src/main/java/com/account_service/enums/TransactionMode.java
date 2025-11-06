package com.account_service.enums;

public enum TransactionMode {
	ONLINE, // Online banking
	MOBILE_APP, // Mobile banking app
	ATM, // ATM transaction
	BRANCH, // Bank branch
	POS, // Point of Sale
	API, // API/Integration
	AUTOMATED // System automated (interest, fees, etc.)
}
