package pt.lunasoft.fraud.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.models.FraudAlert;
import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.AlertStatus;
import pt.lunasoft.models.enums.FraudSeverity;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

	private final TransactionHistoryService historyService;
    //private final FraudRuleEngine ruleEngine;

    public FraudAlert analyzeTransaction(Transaction transaction) {
        log.info("Analyzing transaction: {} for account: {}", transaction.getId(), transaction.getAccountId());

        List<String> triggeredRules = new ArrayList<>();
        double riskScore = 0.0;

        // Rule 1: High amount transaction
        if (isHighAmountTransaction(transaction)) {
            triggeredRules.add("HIGH_AMOUNT");
            riskScore += 30.0;
        }

        // Rule 2: Unusual location
        if (isUnusualLocation(transaction)) {
            triggeredRules.add("UNUSUAL_LOCATION");
            riskScore += 25.0;
        }

        // Rule 3: Rapid succession transactions
        if (isRapidSuccession(transaction)) {
            triggeredRules.add("RAPID_SUCCESSION");
            riskScore += 35.0;
        }

        // Rule 4: Unusual time
        if (isUnusualTime(transaction)) {
            triggeredRules.add("UNUSUAL_TIME");
            riskScore += 15.0;
        }

        // Rule 5: Velocity check
        if (exceedsVelocityLimit(transaction)) {
            triggeredRules.add("VELOCITY_EXCEEDED");
            riskScore += 40.0;
        }

        // Create alert if risk score exceeds threshold
        if (riskScore >= 50.0 || !triggeredRules.isEmpty()) {
            return createFraudAlert(transaction, riskScore, triggeredRules);
        }

        return null;
    }

    private boolean isHighAmountTransaction(Transaction transaction) {
        return transaction.getAmount().compareTo(new BigDecimal("5000")) > 0;
    }

    private boolean isUnusualLocation(Transaction transaction) {
        if (transaction.getLocation() == null) {
            return false;
        }
        
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 10);
        
        if (recentTransactions.isEmpty()) {
            return false;
        }

        // Check if location differs significantly from recent transactions
        String currentCountry = transaction.getLocation().getCountry();
        long sameCountryCount = recentTransactions.stream()
                .filter(t -> t.getLocation() != null)
                .filter(t -> currentCountry.equals(t.getLocation().getCountry()))
                .count();

        return sameCountryCount == 0;
    }

    private boolean isRapidSuccession(Transaction transaction) {
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 5);
        
        if (recentTransactions.isEmpty()) {
            return false;
        }

        Transaction lastTransaction = recentTransactions.get(0);
        Duration timeDiff = Duration.between(lastTransaction.getTimestamp(), transaction.getTimestamp());

        return timeDiff.toMinutes() < 2;
    }

    private boolean isUnusualTime(Transaction transaction) {
        int hour = transaction.getTimestamp().atZone(java.time.ZoneId.systemDefault()).getHour();
        
        // Flag transactions between 2 AM and 5 AM as unusual
        return hour >= 2 && hour < 5;
    }

    private boolean exceedsVelocityLimit(Transaction transaction) {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        
        BigDecimal totalAmount = historyService.getTotalAmountSince(transaction.getAccountId(), oneHourAgo);
        
        totalAmount = totalAmount.add(transaction.getAmount());
        
        return totalAmount.compareTo(new BigDecimal("10000")) > 0;
    }

    private FraudAlert createFraudAlert(Transaction transaction, double riskScore, List<String> triggeredRules) {
        
        FraudSeverity severity = determineSeverity(riskScore);
        
        return FraudAlert.builder()
                .id(UUID.randomUUID())
                .transactionId(transaction.getId())
                .accountId(transaction.getAccountId())
                .severity(severity)
                .riskScore(riskScore)
                .triggeredRules(triggeredRules)
                .description(buildDescription(triggeredRules, riskScore))
                .status(AlertStatus.OPEN)
                .detectedAt(Instant.now())
                .build();
    }

    private FraudSeverity determineSeverity(double riskScore) {
        if (riskScore >= 80) {
            return FraudSeverity.CRITICAL;
        } else if (riskScore >= 60) {
            return FraudSeverity.HIGH;
        } else if (riskScore >= 40) {
            return FraudSeverity.MEDIUM;
        }
        return FraudSeverity.LOW;
    }

    private String buildDescription(List<String> rules, double riskScore) {
        return String.format("Fraud detection triggered. Risk score: %.2f. Rules: %s", riskScore, String.join(", ", rules));
    }
	
}