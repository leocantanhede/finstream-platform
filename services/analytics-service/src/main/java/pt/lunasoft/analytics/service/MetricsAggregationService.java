package pt.lunasoft.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.analytics.model.AccountMetrics;
import pt.lunasoft.analytics.model.RealTimeMetrics;
import pt.lunasoft.analytics.model.TimeSeriesDataPoint;
import pt.lunasoft.models.Transaction;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsAggregationService {

    private final InfluxDBService influxDBService;
    private final ReactiveRedisTemplate<String, AccountMetrics> redisTemplate;

    private final Map<String, AccountMetrics> accountMetricsCache = new ConcurrentHashMap<>();
    private final AtomicLong globalTransactionCount = new AtomicLong(0);
    private final AtomicLong globalFraudCount = new AtomicLong(0);

    public void processTransaction(Transaction transaction) {
        // Update account metrics
        updateAccountMetrics(transaction);

        // Write to time series database
        writeTimeSeriesData(transaction);

        // Update global counters
        globalTransactionCount.incrementAndGet();
    }

    private void updateAccountMetrics(Transaction transaction) {
        String accountId = transaction.getAccountId();
        
        AccountMetrics metrics = accountMetricsCache.computeIfAbsent(
                accountId, 
                k -> AccountMetrics.builder()
                        .accountId(accountId)
                        .totalTransactions(0L)
                        .totalSpending(BigDecimal.ZERO)
                        .uniqueMerchants(0)
                        .transactionsByType(new HashMap<>())
                        .spendingByCategory(new HashMap<>())
                        .fraudAlertsCount(0L)
                        .fraudRate(0.0)
                        .build()
        );

        // Update metrics
        metrics.setTotalTransactions(metrics.getTotalTransactions() + 1);
        metrics.setTotalSpending(metrics.getTotalSpending().add(transaction.getAmount()));
        metrics.setAverageTransactionAmount(metrics.getTotalSpending().divide(new BigDecimal(metrics.getTotalTransactions()), 2, RoundingMode.HALF_UP));

        // Update transaction type count
        String type = transaction.getType().toString();
        metrics.getTransactionsByType().merge(type, 1L, Long::sum);

        // Update spending by category
        if (transaction.getMerchantCategory() != null) {
            metrics.getSpendingByCategory().merge(
                    transaction.getMerchantCategory(), 
                    transaction.getAmount(), 
                    BigDecimal::add
            );
        }

        metrics.setLastUpdated(Instant.now());

        // Persist to Redis asynchronously
        persistToRedis(accountId, metrics).subscribe();
    }

    private void writeTimeSeriesData(Transaction transaction) {
        TimeSeriesDataPoint dataPoint = TimeSeriesDataPoint.builder()
                .metric("transaction")
                .value(transaction.getAmount())
                .timestamp(transaction.getTimestamp())
                .tags(Map.of(
                        "account_id", transaction.getAccountId(),
                        "type", transaction.getType().toString(),
                        "currency", transaction.getCurrency(),
                        "merchant_category", transaction.getMerchantCategory() != null ? transaction.getMerchantCategory() : "unknown"))
                .build();

        influxDBService.writeDataPoint(dataPoint);
    }

    public Mono<AccountMetrics> getAccountMetrics(String accountId) {
        // Try cache first
        AccountMetrics cached = accountMetricsCache.get(accountId);
        if (cached != null) {
            return Mono.just(cached);
        }

        // Fallback to Redis
        return redisTemplate.opsForValue()
                .get("account:metrics:" + accountId)
                .doOnNext(metrics -> accountMetricsCache.put(accountId, metrics));
    }

    private Mono<Boolean> persistToRedis(String accountId, AccountMetrics metrics) {
        return redisTemplate.opsForValue()
                .set("account:metrics:" + accountId, metrics, Duration.ofHours(24))
                .doOnSuccess(success -> log.debug("Persisted metrics for account: {}", accountId))
                .onErrorResume(error -> {
                    log.error("Failed to persist metrics to Redis", error);
                    return Mono.just(false);
                });
    }

    public RealTimeMetrics getGlobalMetrics() {
        return RealTimeMetrics.builder()
                .totalTransactions(globalTransactionCount.get())
                .fraudAlertsCount(globalFraudCount.get())
                .fraudRate(calculateFraudRate())
                .timestamp(Instant.now())
                .build();
    }

    private Double calculateFraudRate() {
        long total = globalTransactionCount.get();
        long fraud = globalFraudCount.get();
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) fraud / total * 100;
    }

    public void incrementFraudCount() {
        globalFraudCount.incrementAndGet();
    }
    
}