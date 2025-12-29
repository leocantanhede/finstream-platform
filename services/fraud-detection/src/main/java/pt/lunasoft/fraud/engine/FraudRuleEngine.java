package pt.lunasoft.fraud.engine;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.fraud.service.TransactionHistoryService;
import pt.lunasoft.models.Transaction;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudRuleEngine {

    private final TransactionHistoryService historyService;
    
    // Cache for tracking patterns
    private final Map<String, List<Transaction>> recentTransactionsCache = new ConcurrentHashMap<>();
    
    // Thresholds
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000");
    private static final int MAX_TRANSACTIONS_PER_HOUR = 10;
    private static final int MAX_TRANSACTIONS_PER_DAY = 50;
    private static final BigDecimal MAX_DAILY_AMOUNT = new BigDecimal("20000");
    
    /**
     * Evaluate all fraud rules for a transaction
     * @param transaction The transaction to evaluate
     * @return Map of rule names to their scores
     */
    public Map<String, Double> evaluateAllRules(Transaction transaction) {
        Map<String, Double> ruleScores = new ConcurrentHashMap<>();
        
        // Execute all rules
        ruleScores.put("HIGH_AMOUNT", evaluateHighAmountRule(transaction));
        ruleScores.put("UNUSUAL_LOCATION", evaluateUnusualLocationRule(transaction));
        ruleScores.put("RAPID_SUCCESSION", evaluateRapidSuccessionRule(transaction));
        ruleScores.put("UNUSUAL_TIME", evaluateUnusualTimeRule(transaction));
        ruleScores.put("VELOCITY_CHECK", evaluateVelocityRule(transaction));
        ruleScores.put("DUPLICATE_TRANSACTION", evaluateDuplicateTransactionRule(transaction));
        ruleScores.put("UNUSUAL_MERCHANT", evaluateUnusualMerchantRule(transaction));
        ruleScores.put("DEVICE_FINGERPRINT", evaluateDeviceFingerprintRule(transaction));
        ruleScores.put("ROUND_AMOUNT", evaluateRoundAmountRule(transaction));
        ruleScores.put("GEOGRAPHIC_IMPOSSIBLE", evaluateGeographicImpossibleRule(transaction));
        
        // Log evaluation results
        log.debug("Fraud rules evaluation for transaction {}: {}", transaction.getId(), ruleScores);
        
        return ruleScores;
    }
    
    /**
     * Rule 1: High Amount Transaction
     * Checks if transaction amount exceeds normal thresholds
     */
    public double evaluateHighAmountRule(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        
        if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) > 0) {
            log.warn("Very high amount detected: {} for account: {}", amount, transaction.getAccountId());
            return 40.0; // Very high risk
        } else if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            log.info("High amount detected: {} for account: {}", amount, transaction.getAccountId());
            return 25.0; // Moderate risk
        }
        
        return 0.0; // No risk
    }
    
    /**
     * Rule 2: Unusual Location
     * Checks if transaction location differs significantly from recent transactions
     */
    public double evaluateUnusualLocationRule(Transaction transaction) {
        if (transaction.getLocation() == null || transaction.getLocation().getCountry() == null) {
            return 0.0; // Cannot evaluate without location
        }
        
        String currentCountry = transaction.getLocation().getCountry();
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 20);
        
        if (recentTransactions.isEmpty()) {
            return 0.0; // No history to compare
        }
        
        // Count transactions in same country
        long sameCountryCount = recentTransactions.stream()
                .filter(t -> t.getLocation() != null && t.getLocation().getCountry() != null)
                .filter(t -> currentCountry.equals(t.getLocation().getCountry()))
                .count();
        
        long totalWithLocation = recentTransactions.stream()
                .filter(t -> t.getLocation() != null && t.getLocation().getCountry() != null)
                .count();
        
        if (totalWithLocation == 0) {
            return 0.0;
        }
        
        double percentageSameCountry = (double) sameCountryCount / totalWithLocation;
        
        if (percentageSameCountry == 0) {
            log.warn("Transaction from completely new country: {} for account: {}", currentCountry, transaction.getAccountId());
            return 35.0; // High risk
        } else if (percentageSameCountry < 0.2) {
            return 20.0; // Moderate risk
        }
        
        return 0.0; // Normal pattern
    }
    
    /**
     * Rule 3: Rapid Succession
     * Checks if transactions are occurring too quickly
     */
    public double evaluateRapidSuccessionRule(Transaction transaction) {
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 5);
        
        if (recentTransactions.isEmpty()) {
            return 0.0;
        }
        
        Transaction lastTransaction = recentTransactions.get(0);
        Duration timeDiff = Duration.between(lastTransaction.getTimestamp(), transaction.getTimestamp());
        
        long secondsBetween = timeDiff.getSeconds();
        
        if (secondsBetween < 30) {
            log.warn("Transaction within 30 seconds of previous for account: {}", transaction.getAccountId());
            return 45.0; // Very high risk
        } else if (secondsBetween < 120) {
            log.info("Transaction within 2 minutes of previous for account: {}", transaction.getAccountId());
            return 30.0; // Moderate risk
        } else if (secondsBetween < 300) {
            return 15.0; // Low risk
        }
        
        return 0.0; // Normal timing
    }
    
    /**
     * Rule 4: Unusual Time
     * Checks if transaction occurs at an unusual hour
     */
    public double evaluateUnusualTimeRule(Transaction transaction) {
        ZonedDateTime zonedDateTime = transaction.getTimestamp().atZone(java.time.ZoneId.systemDefault());
        int hour = zonedDateTime.getHour();
        
        // Late night transactions (2 AM - 5 AM) are more suspicious
        if (hour >= 2 && hour < 5) {
            log.info("Late night transaction at {}:00 for account: {}", hour, transaction.getAccountId());
            return 20.0;
        }
        
        // Very early morning (5 AM - 6 AM)
        if (hour >= 5 && hour < 6) {
            return 10.0;
        }
        
        return 0.0; // Normal hours
    }
    
    /**
     * Rule 5: Velocity Check
     * Checks transaction velocity (count and amount) over time periods
     */
    public double evaluateVelocityRule(Transaction transaction) {
        String accountId = transaction.getAccountId();
        Instant now = Instant.now();
        
        // Check hourly velocity
        Instant oneHourAgo = now.minus(Duration.ofHours(1));
        long hourlyCount = historyService.getRecentTransactions(accountId, 100).stream()
                .filter(t -> t.getTimestamp().isAfter(oneHourAgo))
                .count();
        
        BigDecimal hourlyAmount = historyService.getTotalAmountSince(accountId, oneHourAgo);
        hourlyAmount = hourlyAmount.add(transaction.getAmount());
        
        // Check daily velocity
        Instant oneDayAgo = now.minus(Duration.ofDays(1));
        long dailyCount = historyService.getRecentTransactions(accountId, 100).stream()
                .filter(t -> t.getTimestamp().isAfter(oneDayAgo))
                .count();
        
        BigDecimal dailyAmount = historyService.getTotalAmountSince(accountId, oneDayAgo);
        dailyAmount = dailyAmount.add(transaction.getAmount());
        
        double riskScore = 0.0;
        
        // Evaluate hourly count
        if (hourlyCount > MAX_TRANSACTIONS_PER_HOUR) {
            log.warn("Excessive hourly transactions: {} for account: {}", hourlyCount, accountId);
            riskScore += 30.0;
        } else if (hourlyCount > MAX_TRANSACTIONS_PER_HOUR * 0.7) {
            riskScore += 15.0;
        }
        
        // Evaluate daily count
        if (dailyCount > MAX_TRANSACTIONS_PER_DAY) {
            log.warn("Excessive daily transactions: {} for account: {}", dailyCount, accountId);
            riskScore += 25.0;
        } else if (dailyCount > MAX_TRANSACTIONS_PER_DAY * 0.8) {
            riskScore += 10.0;
        }
        
        // Evaluate daily amount
        if (dailyAmount.compareTo(MAX_DAILY_AMOUNT) > 0) {
            log.warn("Excessive daily amount: {} for account: {}", dailyAmount, accountId);
            riskScore += 35.0;
        }
        
        return Math.min(riskScore, 100.0); // Cap at 100
    }
    
    /**
     * Rule 6: Duplicate Transaction
     * Checks for potential duplicate/repeated transactions
     */
    public double evaluateDuplicateTransactionRule(Transaction transaction) {
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 10);
        
        if (recentTransactions.isEmpty()) {
            return 0.0;
        }
        
        // Look for duplicate amount and merchant within last 5 minutes
        Instant fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(5));
        
        long duplicateCount = recentTransactions.stream()
                .filter(t -> t.getTimestamp().isAfter(fiveMinutesAgo))
                .filter(t -> t.getAmount().compareTo(transaction.getAmount()) == 0)
                .filter(t -> t.getMerchant().equalsIgnoreCase(transaction.getMerchant()))
                .count();
        
        if (duplicateCount > 0) {
            log.warn("Potential duplicate transaction detected for account: {}, merchant: {}, amount: {}", 
                    transaction.getAccountId(), 
                    transaction.getMerchant(), 
                    transaction.getAmount());
            return 40.0;
        }
        
        return 0.0;
    }
    
    /**
     * Rule 7: Unusual Merchant
     * Checks if merchant is unusual for this account
     */
    public double evaluateUnusualMerchantRule(Transaction transaction) {
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 50);
        
        if (recentTransactions.size() < 10) {
            return 0.0; // Not enough history
        }
        
        String currentMerchant = transaction.getMerchant();
        
        // Check if this merchant has been used before
        boolean merchantFound = recentTransactions.stream().anyMatch(t -> t.getMerchant().equalsIgnoreCase(currentMerchant));
        
        if (!merchantFound) {
            // Check if merchant category is unusual
            String currentCategory = transaction.getMerchantCategory();
            if (currentCategory != null) {
                long categoryCount = recentTransactions.stream()
                        .filter(t -> currentCategory.equals(t.getMerchantCategory()))
                        .count();
                
                if (categoryCount == 0) {
                    log.info("New merchant and new category for account: {}", transaction.getAccountId());
                    return 25.0;
                } else {
                    return 10.0; // New merchant but familiar category
                }
            }
            
            return 15.0; // New merchant
        }
        
        return 0.0;
    }
    
    /**
     * Rule 8: Device Fingerprint Check
     * Checks if transaction is from a known device
     */
    public double evaluateDeviceFingerprintRule(Transaction transaction) {
        if (transaction.getDeviceInfo() == null || transaction.getDeviceInfo().getDeviceId() == null) {
            return 5.0; // Slight risk for missing device info
        }
        
        String currentDeviceId = transaction.getDeviceInfo().getDeviceId();
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 30);
        
        if (recentTransactions.isEmpty()) {
            return 0.0;
        }
        
        // Check if device has been used before
        boolean deviceFound = recentTransactions.stream()
                .filter(t -> t.getDeviceInfo() != null)
                .anyMatch(t -> currentDeviceId.equals(t.getDeviceInfo().getDeviceId()));
        
        if (!deviceFound) {
            log.info("Transaction from new device: {} for account: {}", currentDeviceId, transaction.getAccountId());
            return 20.0;
        }
        
        return 0.0;
    }
    
    /**
     * Rule 9: Round Amount Check
     * Round amounts (like $1000.00) can indicate testing or fraud
     */
    public double evaluateRoundAmountRule(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        
        // Check if amount is exactly a round number
        if (amount.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0 &&
            amount.compareTo(new BigDecimal("1000")) >= 0) {
            log.debug("Round amount detected: {} for account: {}", amount, transaction.getAccountId());
            return 15.0;
        }
        
        if (amount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) == 0 &&
            amount.compareTo(new BigDecimal("500")) >= 0) {
            return 8.0;
        }
        
        return 0.0;
    }
    
    /**
     * Rule 10: Geographically Impossible Travel
     * Checks if travel time between locations is physically impossible
     */
    public double evaluateGeographicImpossibleRule(Transaction transaction) {
        if (transaction.getLocation() == null || transaction.getLocation().getLatitude() == null || transaction.getLocation().getLongitude() == null) {
            return 0.0;
        }
        
        List<Transaction> recentTransactions = historyService.getRecentTransactions(transaction.getAccountId(), 5);
        
        if (recentTransactions.isEmpty()) {
            return 0.0;
        }
        
        // Find last transaction with location
        Transaction lastTransactionWithLocation = recentTransactions.stream()
                .filter(t -> t.getLocation() != null && 
                           t.getLocation().getLatitude() != null && 
                           t.getLocation().getLongitude() != null)
                .findFirst()
                .orElse(null);
        
        if (lastTransactionWithLocation == null) {
            return 0.0;
        }
        
        // Calculate distance
        double distance = calculateDistance(
                lastTransactionWithLocation.getLocation().getLatitude(),
                lastTransactionWithLocation.getLocation().getLongitude(),
                transaction.getLocation().getLatitude(),
                transaction.getLocation().getLongitude()
        );
        
        // Calculate time difference in hours
        Duration timeDiff = Duration.between(lastTransactionWithLocation.getTimestamp(),transaction.getTimestamp());
        double hoursDiff = timeDiff.toMinutes() / 60.0;
        
        if (hoursDiff <= 0) {
            return 0.0;
        }
        
        // Calculate required speed (km/h)
        double requiredSpeed = distance / hoursDiff;
        
        // Maximum realistic speed (considering flights): 900 km/h
        if (requiredSpeed > 900) {
            log.warn("Geographically impossible travel detected: {} km in {} hours ({} km/h) for account: {}", distance, hoursDiff, requiredSpeed, transaction.getAccountId());
            return 50.0; // Very high risk
        } else if (requiredSpeed > 500) {
            // Possible but requires air travel
            return 25.0;
        }
        
        return 0.0;
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius in kilometers
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
    
    /**
     * Calculate overall risk score from individual rule scores
     * @param ruleScores Map of rule names to their scores
     * @return Overall risk score (0-100)
     */
    public double calculateOverallRiskScore(Map<String, Double> ruleScores) {
        if (ruleScores.isEmpty()) {
            return 0.0;
        }
        
        // Weighted average approach
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        
        // Define weights for each rule (higher weight = more important)
        Map<String, Double> ruleWeights = Map.of(
                "HIGH_AMOUNT", 1.5,
                "UNUSUAL_LOCATION", 1.2,
                "RAPID_SUCCESSION", 1.8,
                "UNUSUAL_TIME", 0.8,
                "VELOCITY_CHECK", 1.5,
                "DUPLICATE_TRANSACTION", 1.6,
                "UNUSUAL_MERCHANT", 1.0,
                "DEVICE_FINGERPRINT", 1.1,
                "ROUND_AMOUNT", 0.7,
                "GEOGRAPHIC_IMPOSSIBLE", 2.0
        );
        
        for (Map.Entry<String, Double> entry : ruleScores.entrySet()) {
            String ruleName = entry.getKey();
            Double score = entry.getValue();
            Double weight = ruleWeights.getOrDefault(ruleName, 1.0);
            
            totalWeightedScore += score * weight;
            totalWeight += weight;
        }
        
        double averageScore = totalWeightedScore / totalWeight;
        
        // Apply non-linear scaling to emphasize higher risks
        // Use sigmoid-like function
        double scaledScore = 100 * (1 / (1 + Math.exp(-0.08 * (averageScore - 50))));
        
        return Math.min(Math.max(scaledScore, 0.0), 100.0);
    }
    
    /**
     * Get triggered rules (rules with score > 0)
     * @param ruleScores Map of rule names to their scores
     * @return List of triggered rule names
     */
    public List<String> getTriggeredRules(Map<String, Double> ruleScores) {
        List<String> triggeredRules = new ArrayList<>();
        
        ruleScores.forEach((ruleName, score) -> {
            if (score > 0) {
                triggeredRules.add(ruleName);
            }
        });
        
        return triggeredRules;
    }
    
    /**
     * Clear cache for specific account (for testing/maintenance)
     */
    public void clearCache(String accountId) {
        recentTransactionsCache.remove(accountId);
        log.debug("Cache cleared for account: {}", accountId);
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        recentTransactionsCache.clear();
        log.info("All caches cleared");
    }
    
}