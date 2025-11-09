package com.transaction_service.enums;

public enum TransactionType {
	DEPOSIT, // Cash/cheque deposit
	WITHDRAWAL, // Cash withdrawal
	TRANSFER, // Internal transfer (same bank)
	FUND_TRANSFER, // External transfer (different bank)
	UPI_PAYMENT, // UPI payment
	NEFT, // NEFT transfer
	RTGS, // RTGS transfer
	IMPS, // IMPS transfer
	BILL_PAYMENT, // Bill payment
	RECHARGE, // Mobile/DTH recharge
	LOAN_EMI, // Loan EMI payment
	CREDIT_CARD_PAYMENT, // Credit card payment
	INTEREST_CREDIT, // Interest earned
	REVERSAL, // Transaction reversal
	REFUND, // Refund
	FEE_DEDUCTION, // Bank charges
	SALARY_CREDIT, // Salary credit
	DIVIDEND, // Dividend payment
	CASHBACK, // Cashback credit
	MERCHANT_PAYMENT // Merchant payment
}
