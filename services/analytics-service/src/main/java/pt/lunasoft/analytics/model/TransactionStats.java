package pt.lunasoft.analytics.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStats {

	private String accountId;
    private Long transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Instant windowStart;
    private Instant windowEnd;
    private String merchantCategory;
	
}