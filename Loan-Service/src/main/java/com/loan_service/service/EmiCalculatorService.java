package com.loan_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.loan_service.dto.EmiCalculationResult;
import com.loan_service.enums.InterestType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmiCalculatorService {
	public EmiCalculationResult calculateEmi(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            Integer tenureMonths,
            InterestType interestType) {
        
        return switch (interestType) {
            case REDUCING_BALANCE -> calculateReducingBalanceEmi(
                principal, annualInterestRate, tenureMonths);
            case FLAT_RATE -> calculateFlatRateEmi(
                principal, annualInterestRate, tenureMonths);
            case BULLET_PAYMENT -> calculateBulletPayment(
                principal, annualInterestRate, tenureMonths);
            default -> calculateReducingBalanceEmi(
                principal, annualInterestRate, tenureMonths);
        };
    }

    private EmiCalculationResult calculateReducingBalanceEmi(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            Integer tenureMonths) {
        
        // Monthly interest rate
        BigDecimal monthlyRate = annualInterestRate
            .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        
        // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenureMonths);
        
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        
        BigDecimal emiAmount = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        BigDecimal totalPayment = emiAmount.multiply(new BigDecimal(tenureMonths));
        BigDecimal totalInterest = totalPayment.subtract(principal);
        
        return EmiCalculationResult.builder()
            .emiAmount(emiAmount)
            .totalPayment(totalPayment)
            .totalInterest(totalInterest)
            .principalAmount(principal)
            .build();
    }

    private EmiCalculationResult calculateFlatRateEmi(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            Integer tenureMonths) {
        
        // Total interest = P * R * T
        BigDecimal totalInterest = principal
            .multiply(annualInterestRate)
            .multiply(new BigDecimal(tenureMonths))
            .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalPayment = principal.add(totalInterest);
        BigDecimal emiAmount = totalPayment
            .divide(new BigDecimal(tenureMonths), 2, RoundingMode.HALF_UP);
        
        return EmiCalculationResult.builder()
            .emiAmount(emiAmount)
            .totalPayment(totalPayment)
            .totalInterest(totalInterest)
            .principalAmount(principal)
            .build();
    }

    private EmiCalculationResult calculateBulletPayment(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            Integer tenureMonths) {
        
        // Interest only EMI
        BigDecimal monthlyInterest = principal
            .multiply(annualInterestRate)
            .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalInterest = monthlyInterest
            .multiply(new BigDecimal(tenureMonths));
        
        // Last EMI = Principal + last month interest
        BigDecimal lastEmi = principal.add(monthlyInterest);
        
        return EmiCalculationResult.builder()
            .emiAmount(monthlyInterest) // Regular EMI (interest only)
            .lastEmiAmount(lastEmi) // Last EMI includes principal
            .totalPayment(principal.add(totalInterest))
            .totalInterest(totalInterest)
            .principalAmount(principal)
            .build();
    }

    public BigDecimal calculateLatePaymentCharges(
            BigDecimal emiAmount,
            Integer daysOverdue,
            BigDecimal penaltyRate) {
        
        // Penalty = EMI * (penalty_rate/100) * (days_overdue/30)
        return emiAmount
            .multiply(penaltyRate)
            .multiply(new BigDecimal(daysOverdue))
            .divide(new BigDecimal("3000"), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePrepaymentCharges(
            BigDecimal prepaymentAmount,
            BigDecimal chargesPercentage) {
        
        return prepaymentAmount
            .multiply(chargesPercentage)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
