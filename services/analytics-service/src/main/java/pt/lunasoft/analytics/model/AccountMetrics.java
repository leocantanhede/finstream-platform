package pt.lunasoft.analytics.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountMetrics {

	private String accountId;
    private Long totalTransactions;
    private BigDecimal totalSpending;
    private BigDecimal averageTransactionAmount;
    private Integer uniqueMerchants;
    private Map<String, Long> transactionsByType;
    private Map<String, BigDecimal> spendingByCategory;
    private Long fraudAlertsCount;
    private Double fraudRate;
    private Instant lastUpdated;
	
}