package com.fraud_detection.rules;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fraud_detection.dto.TransactionEventDTO;
import com.fraud_detection.repository.FraudCheckRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudRulesEngine {
	 private final FraudCheckRepository fraudCheckRepository;
	    private final RedisTemplate<String, Object> redisTemplate;
	    
	    @Value("${fraud.detection.velocity.max-transactions-per-hour}")
	    private int maxTransactionsPerHour;
	    
	    @Value("${fraud.detection.velocity.max-transactions-per-day}")
	    private int maxTransactionsPerDay;
	    
	    @Value("${fraud.detection.velocity.max-amount-per-hour}")
	    private BigDecimal maxAmountPerHour;
	    
	    @Value("${fraud.detection.velocity.max-amount-per-day}")
	    private BigDecimal maxAmountPerDay;
	    
	    @Value("${fraud.detection.geographic.suspicious-countries}")
	    private String suspiciousCountries;
	    
	    @Value("${fraud.detection.geographic.max-distance-km}")
	    private double maxDistanceKm;
	    
	    @Value("${fraud.detection.geographic.max-time-between-locations-minutes}")
	    private int maxTimeBetweenLocations;
	    
	    @Value("${fraud.detection.amount.high-value-threshold}")
	    private BigDecimal highValueThreshold;
	    
	    @Value("${fraud.detection.amount.unusual-multiplier}")
	    private double unusualMultiplier;
	    
	    @Value("${fraud.detection.time.unusual-hours-start}")
	    private int unusualHoursStart;
	    
	    @Value("${fraud.detection.time.unusual-hours-end}")
	    private int unusualHoursEnd;
	    
	    public RuleCheckResult evaluateTransaction(TransactionEventDTO transaction) {
	        List<String> violations = new ArrayList<>();
	        double riskScore = 0.0;
	        
	        // Rule 1: Velocity Check
	        RuleResult velocityResult = checkVelocity(transaction);
	        if (!velocityResult.isPassed()) {
	            violations.addAll(velocityResult.getReasons());
	            riskScore += velocityResult.getRiskScore();
	        }
	        
	        // Rule 2: Geographic Check
	        RuleResult geographicResult = checkGeographic(transaction);
	        if (!geographicResult.isPassed()) {
	            violations.addAll(geographicResult.getReasons());
	            riskScore += geographicResult.getRiskScore();
	        }
	        
	        // Rule 3: Amount Check
	        RuleResult amountResult = checkAmount(transaction);
	        if (!amountResult.isPassed()) {
	            violations.addAll(amountResult.getReasons());
	            riskScore += amountResult.getRiskScore();
	        }
	        
	        // Rule 4: Time-based Check
	        RuleResult timeResult = checkUnusualTime(transaction);
	        if (!timeResult.isPassed()) {
	            violations.addAll(timeResult.getReasons());
	            riskScore += timeResult.getRiskScore();
	        }
	        
	        // Rule 5: Device/IP Check
	        RuleResult deviceResult = checkDevice(transaction);
	        if (!deviceResult.isPassed()) {
	            violations.addAll(deviceResult.getReasons());
	            riskScore += deviceResult.getRiskScore();
	        }
	        
	        // Rule 6: Duplicate Transaction Check
	        RuleResult duplicateResult = checkDuplicateTransaction(transaction);
	        if (!duplicateResult.isPassed()) {
	            violations.addAll(duplicateResult.getReasons());
	            riskScore += duplicateResult.getRiskScore();
	        }
	        
	        // Normalize risk score to 0-1 range
	        riskScore = Math.min(riskScore, 1.0);
	        
	        return RuleCheckResult.builder()
	                .passed(violations.isEmpty())
	                .riskScore(riskScore)
	                .violations(violations)
	                .build();
	    }
	    
	    private RuleResult checkVelocity(TransactionEventDTO transaction) {
	        List<String> reasons = new ArrayList<>();
	        double riskScore = 0.0;
	        
	        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
	        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
	        
	        // Check transaction count per hour
	        Long transactionsLastHour = fraudCheckRepository
	                .countTransactionsByAccountInTimeRange(
	                        transaction.getAccountId(), oneHourAgo, LocalDateTime.now());
	        
	        if (transactionsLastHour >= maxTransactionsPerHour) {
	            reasons.add(String.format("Exceeded max transactions per hour: %d/%d", 
	                    transactionsLastHour, maxTransactionsPerHour));
	            riskScore += 0.3;
	        }
	        
	        // Check transaction count per day
	        Long transactionsLastDay = fraudCheckRepository
	                .countTransactionsByAccountInTimeRange(
	                        transaction.getAccountId(), oneDayAgo, LocalDateTime.now());
	        
	        if (transactionsLastDay >= maxTransactionsPerDay) {
	            reasons.add(String.format("Exceeded max transactions per day: %d/%d", 
	                    transactionsLastDay, maxTransactionsPerDay));
	            riskScore += 0.2;
	        }
	        
	        // Check amount per hour
	        BigDecimal amountLastHour = fraudCheckRepository
	                .sumAmountByAccountInTimeRange(
	                        transaction.getAccountId(), oneHourAgo, LocalDateTime.now());
	        
	        if (amountLastHour.compareTo(maxAmountPerHour) > 0) {
	            reasons.add(String.format("Exceeded max amount per hour: %s/%s", 
	                    amountLastHour, maxAmountPerHour));
	            riskScore += 0.3;
	        }
	        
	        // Check amount per day
	        BigDecimal amountLastDay = fraudCheckRepository
	                .sumAmountByAccountInTimeRange(
	                        transaction.getAccountId(), oneDayAgo, LocalDateTime.now());
	        
	        if (amountLastDay.compareTo(maxAmountPerDay) > 0) {
	            reasons.add(String.format("Exceeded max amount per day: %s/%s", 
	                    amountLastDay, maxAmountPerDay));
	            riskScore += 0.2;
	        }
	        
	        return RuleResult.builder()
	                .passed(reasons.isEmpty())
	                .reasons(reasons)
	                .riskScore(riskScore)
	                .build();
	    }
	    
	    private RuleResult checkGeographic(TransactionEventDTO transaction) {
	        List<String> reasons = new ArrayList<>();
	        double riskScore = 0.0;

	        if (transaction.getLocationCountry() != null) {
	            List<String> suspiciousCountryList = Arrays.asList(suspiciousCountries.split(","));
	            if (suspiciousCountryList.contains(transaction.getLocationCountry())) {
	                reasons.add("Transaction from suspicious country: " + transaction.getLocationCountry());
	                riskScore += 0.4;
	            }
	        }

	        if (transaction.getLatitude() != null && transaction.getLongitude() != null) {

	            var lastOpt = fraudCheckRepository.findLatestByAccountId(transaction.getAccountId());

	            if (lastOpt.isPresent()) {
	                var lastTransaction = lastOpt.get();

	                if (lastTransaction.getLatitude() != null && lastTransaction.getLongitude() != null) {

	                    double distance = calculateDistance(
	                            lastTransaction.getLatitude(), lastTransaction.getLongitude(),
	                            transaction.getLatitude(), transaction.getLongitude()
	                    );

	                    long minutesBetween = Duration.between(
	                            lastTransaction.getCreatedAt(),
	                            transaction.getTimestamp()
	                    ).toMinutes();

	                    if (distance > maxDistanceKm && minutesBetween < maxTimeBetweenLocations) {
	                        reasons.add(String.format("Impossible travel: %.2f km in %d minutes",
	                                distance, minutesBetween));
	                        riskScore += 0.5;
	                    }
	                }
	            }
	        }

	        return RuleResult.builder()
	                .passed(reasons.isEmpty())
	                .reasons(reasons)
	                .riskScore(riskScore)
	                .build();
	    }

	    
	    private RuleResult checkAmount(TransactionEventDTO transaction) {
	        List<String> reasons = new ArrayList<>();
	        double riskScore = 0.0;
	        
	        // Check high-value transaction
	        if (transaction.getAmount().compareTo(highValueThreshold) > 0) {
	            reasons.add("High-value transaction: " + transaction.getAmount());
	            riskScore += 0.3;
	        }
	        
	        // Check unusual amount (compared to user's average)
	        String avgKey = "avg_amount:" + transaction.getAccountId();
	        Double avgAmount = (Double) redisTemplate.opsForValue().get(avgKey);
	        
	        if (avgAmount != null) {
	            double currentAmount = transaction.getAmount().doubleValue();
	            if (currentAmount > avgAmount * unusualMultiplier) {
	                reasons.add(String.format(
	                        "Unusual amount: %.2f (avg: %.2f)", 
	                        currentAmount, avgAmount));
	                riskScore += 0.25;
	            }
	        }
	        
	        return RuleResult.builder()
	                .passed(reasons.isEmpty())
	                .reasons(reasons)
	                .riskScore(riskScore)
	                .build();
	    }
	    
	    private RuleResult checkUnusualTime(TransactionEventDTO transaction) {
	        List<String> reasons = new ArrayList<>();
	        double riskScore = 0.0;
	        
	        int hour = transaction.getTimestamp() != null ? 
	                transaction.getTimestamp().getHour() : LocalDateTime.now().getHour();
	        
	        if (hour >= unusualHoursStart && hour < unusualHoursEnd) {
	            reasons.add("Transaction during unusual hours: " + hour + ":00");
	            riskScore += 0.15;
	        }
	        
	        return RuleResult.builder()
	                .passed(reasons.isEmpty())
	                .reasons(reasons)
	                .riskScore(riskScore)
	                .build();
	    }
	    
	    private RuleResult checkDevice(TransactionEventDTO transaction) {
	        List<String> reasons = new ArrayList<>();
	        double riskScore = 0.0;
	        
	        if (transaction.getDeviceId() != null) {
	            String deviceKey = "device:" + transaction.getAccountId();
	            String knownDevice = (String) redisTemplate.opsForValue().get(deviceKey);
	            
	            if (knownDevice != null && !knownDevice.equals(transaction.getDeviceId())) {
	                reasons.add("New device detected: " + transaction.getDeviceId());
	                riskScore += 0.2;
	            }
	        }
	        
	        return RuleResult.builder()
	                .passed(reasons.isEmpty())
	                .reasons(reasons)
	                .riskScore(riskScore)
	                .build();
	    }
	    
	    private RuleResult checkDuplicateTransaction(TransactionEventDTO transaction) {
	        List<String> reasons = new ArrayList<>();
	        double riskScore = 0.0;
	        
	        String duplicateKey = "txn:" + transaction.getAccountId() + ":" + 
	                transaction.getAmount() + ":" + transaction.getMerchantName();
	        
	        Boolean isDuplicate = redisTemplate.opsForValue()
	                .setIfAbsent(duplicateKey, "1", 5, TimeUnit.MINUTES);
	        
	        if (Boolean.FALSE.equals(isDuplicate)) {
	            reasons.add("Potential duplicate transaction detected");
	            riskScore += 0.35;
	        }
	        
	        return RuleResult.builder()
	                .passed(reasons.isEmpty())
	                .reasons(reasons)
	                .riskScore(riskScore)
	                .build();
	    }
	    
	    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
	        final int EARTH_RADIUS = 6371; // km
	        
	        double dLat = Math.toRadians(lat2 - lat1);
	        double dLon = Math.toRadians(lon2 - lon1);
	        
	        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
	                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	                Math.sin(dLon / 2) * Math.sin(dLon / 2);
	        
	        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	        
	        return EARTH_RADIUS * c;
	    }
}
