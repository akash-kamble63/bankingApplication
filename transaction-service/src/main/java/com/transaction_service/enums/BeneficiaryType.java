package com.transaction_service.enums;

public enum BeneficiaryType {
	INTERNAL, // Same bank beneficiary
	EXTERNAL, // Different bank beneficiary
	IMPS, // IMPS beneficiary
	NEFT, // NEFT beneficiary
	RTGS, // RTGS beneficiary
	UPI, // UPI beneficiary
	INTERNATIONAL // International beneficiary	
}
