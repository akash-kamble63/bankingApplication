package com.account_service.enums;

public enum TransferMethod {
	INTERNAL,        // Within same bank
    IMPS,            // Immediate Payment Service (instant, 24x7)
    NEFT,            // National Electronic Funds Transfer (batch processing)
    RTGS,            // Real Time Gross Settlement (high value, real-time)
    UPI,             // Unified Payments Interface
    WIRE             // International wire transfer
}
