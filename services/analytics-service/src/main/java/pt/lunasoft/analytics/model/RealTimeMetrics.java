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
public class RealTimeMetrics {

	private Long transactionsPerSecond;
    private BigDecimal volumePerSecond;
    private Long totalTransactions;
    private BigDecimal totalVolume;
    private Long fraudAlertsCount;
    private Double fraudRate;
    private Double averageProcessingTime;
    private Map<String, Long> transactionsByCountry;
    private Map<String, Long> topMerchants;
    private Instant timestamp;
	
}