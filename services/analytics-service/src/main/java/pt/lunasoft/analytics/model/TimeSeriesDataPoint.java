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
public class TimeSeriesDataPoint {

	private String metric;
    private BigDecimal value;
    private Instant timestamp;
    private Map<String, String> tags;
	
}