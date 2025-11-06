package com.account_service.enums;

public enum AccountHolderType {
	INDIVIDUAL, // Single person account
	JOINT, // Joint account (multiple holders)
	MINOR, // Minor account (with guardian)
	CORPORATE, // Company/business account
	TRUST, // Trust account
	PARTNERSHIP // Partnership firm account
}
