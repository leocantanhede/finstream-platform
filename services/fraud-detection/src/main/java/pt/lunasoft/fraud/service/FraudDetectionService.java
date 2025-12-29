package pt.lunasoft.fraud.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.fraud.engine.FraudRuleEngine;
import pt.lunasoft.models.FraudAlert;
import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.AlertStatus;
import pt.lunasoft.models.enums.FraudSeverity;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

	private final TransactionHistoryService historyService;
    private final FraudRuleEngine ruleEngine;

    public FraudAlert analyzeTransaction(Transaction transaction) {
        log.info("Analyzing transaction: {} for account: {}", transaction.getId(), transaction.getAccountId());

        // Add transaction to history first
        historyService.addTransaction(transaction);

        // Evaluate all fraud rules using the rule engine
        Map<String, Double> ruleScores = ruleEngine.evaluateAllRules(transaction);
        
        // Calculate overall risk score
        double riskScore = ruleEngine.calculateOverallRiskScore(ruleScores);
        
        // Get list of triggered rules
        List<String> triggeredRules = ruleEngine.getTriggeredRules(ruleScores);

        // Log detailed analysis
        log.debug("Transaction {} analysis - Risk Score: {}, Triggered Rules: {}", transaction.getId(), riskScore, triggeredRules);

        // Create alert if risk score exceeds threshold
        if (riskScore >= 50.0 || !triggeredRules.isEmpty()) {
            FraudAlert alert = createFraudAlert(transaction, riskScore, triggeredRules);
            log.warn("Fraud alert created: {} for transaction: {} with risk score: {}", alert.getId(), transaction.getId(), riskScore);
            return alert;
        }

        log.info("Transaction {} passed fraud checks with risk score: {}", transaction.getId(), riskScore);
        
        return null;
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
                .detectedAt(java.time.Instant.now())
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
        if (rules.isEmpty()) {
            return String.format("Potential fraud detected with risk score: %.2f", riskScore);
        }
        
        StringBuilder description = new StringBuilder();
        description.append(String.format("Fraud detection triggered. Risk score: %.2f. ", riskScore));
        description.append("Rules violated: ");
        
        // Add human-readable descriptions for each rule
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                description.append(", ");
            }
            description.append(getRuleDescription(rules.get(i)));
        }
        
        return description.toString();
    }

    private String getRuleDescription(String ruleName) {
        return switch (ruleName) {
            case "HIGH_AMOUNT" -> "Unusually high transaction amount";
            case "UNUSUAL_LOCATION" -> "Transaction from unusual location";
            case "RAPID_SUCCESSION" -> "Multiple transactions in rapid succession";
            case "UNUSUAL_TIME" -> "Transaction at unusual time";
            case "VELOCITY_CHECK" -> "Transaction velocity exceeded limits";
            case "DUPLICATE_TRANSACTION" -> "Potential duplicate transaction";
            case "UNUSUAL_MERCHANT" -> "Transaction with unusual merchant";
            case "DEVICE_FINGERPRINT" -> "Transaction from unknown device";
            case "ROUND_AMOUNT" -> "Round amount transaction pattern";
            case "GEOGRAPHIC_IMPOSSIBLE" -> "Geographically impossible travel";
            default -> ruleName;
        };
    }
	
}