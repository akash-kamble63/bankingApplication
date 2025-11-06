package com.account_service.enums;

public enum FeeType {
	ACCOUNT_MAINTENANCE, // Monthly/Annual maintenance fee
	MINIMUM_BALANCE, // Penalty for not maintaining minimum balance
	ATM_WITHDRAWAL, // ATM withdrawal fee (other bank)
	OVERDRAFT, // Overdraft facility fee
	CHEQUE_BOOK, // Cheque book issuance fee
	CARD_ANNUAL, // Card annual fee
	TRANSACTION, // Per transaction fee
	FOREIGN_TRANSACTION, // Foreign transaction fee
	SMS_ALERT, // SMS alert service fee
	STOP_PAYMENT, // Stop payment instruction fee
	LOAN_PROCESSING // Loan processing fee
}
