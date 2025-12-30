package pt.lunasoft.analytics.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.analytics.model.TimeSeriesDataPoint;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsQueryService {

	private final InfluxDBService influxDBService;

	public List<TimeSeriesDataPoint> getTransactionVolumeTimeSeries(Duration duration) {
		String flux = String.format(
				"from(bucket: \"analytics\") " +
						"|> range(start: -%s) " +
						"|> filter(fn: (r) => r._measurement == \"transaction\") " +
						"|> aggregateWindow(every: 1m, fn: sum) " +
						"|> yield(name: \"volume\")",
						formatDuration(duration)
				);
		return influxDBService.query(flux);
	}

	public List<TimeSeriesDataPoint> getTransactionCountTimeSeries(Duration duration) {
		String flux = String.format(
				"from(bucket: \"analytics\") " +
						"|> range(start: -%s) " +
						"|> filter(fn: (r) => r._measurement == \"transaction\") " +
						"|> aggregateWindow(every: 1m, fn: count) " +
						"|> yield(name: \"count\")",
						formatDuration(duration)
				);

		return influxDBService.query(flux);
	}

	public Map<String, Long> getTransactionsByCategory(Duration duration) {
		String flux = String.format(
				"from(bucket: \"analytics\") " +
						"|> range(start: -%s) " +
						"|> filter(fn: (r) => r._measurement == \"transaction\") " +
						"|> group(columns: [\"merchant_category\"]) " +
						"|> count() " +
						"|> yield(name: \"by_category\")",
						formatDuration(duration)
				);

		List<TimeSeriesDataPoint> results = influxDBService.query(flux);

		return results.stream()
				.collect(Collectors.groupingBy(
						dp -> dp.getTags().getOrDefault("merchant_category", "unknown"),
						Collectors.counting()
						));
	}

	public List<TimeSeriesDataPoint> getAverageTransactionAmount(Duration duration, String groupBy) {
		String flux = String.format(
				"from(bucket: \"analytics\") " +
						"|> range(start: -%s) " +
						"|> filter(fn: (r) => r._measurement == \"transaction\") " +
						"|> group(columns: [\"%s\"]) " +
						"|> aggregateWindow(every: 5m, fn: mean) " +
						"|> yield(name: \"average\")",
						formatDuration(duration), groupBy
				);

		return influxDBService.query(flux);
	}

	private String formatDuration(Duration duration) {
		long hours = duration.toHours();
		if (hours > 0) {
			return hours + "h";
		}
		long minutes = duration.toMinutes();
		return minutes + "m";
	}
	
}