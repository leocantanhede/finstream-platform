package pt.lunasoft.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pt.lunasoft.models.enums.AlertStatus;
import pt.lunasoft.models.enums.FraudSeverity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {
	
	private UUID id;
    private UUID transactionId;
    private String accountId;
    private FraudSeverity severity;
    private Double riskScore;
    private List<String> triggeredRules;
    private String description;
    private AlertStatus status;
    private Instant detectedAt;
    private Instant resolvedAt;
    private String resolvedBy;
    private String resolution;
    
}