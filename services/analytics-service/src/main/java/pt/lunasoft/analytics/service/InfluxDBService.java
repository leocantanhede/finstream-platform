package pt.lunasoft.analytics.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.analytics.model.TimeSeriesDataPoint;

@Service
@Slf4j
public class InfluxDBService {

	@Value("${influxdb.url}")
	private String url;

	@Value("${influxdb.token}")
	private String token;

	@Value("${influxdb.org}")
	private String organization;

	@Value("${influxdb.bucket}")
	private String bucket;

	private InfluxDBClient influxDBClient;
	private WriteApiBlocking writeApi;

	@PostConstruct
	public void init() {
		influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket);
		writeApi = influxDBClient.getWriteApiBlocking();
		log.info("InfluxDB client initialized for bucket: {}", bucket);
	}

	@PreDestroy
	public void cleanup() {
		if (influxDBClient != null) {
			influxDBClient.close();
		}
	}

	public void writeDataPoint(TimeSeriesDataPoint dataPoint) {
		try {
			Point point = Point.measurement(dataPoint.getMetric())
					.time(dataPoint.getTimestamp(), WritePrecision.MS)
					.addField("value", dataPoint.getValue().doubleValue());

			if (dataPoint.getTags() != null) {
				dataPoint.getTags().forEach(point::addTag);
			}

			writeApi.writePoint(point);
			log.debug("Written data point: {}", dataPoint.getMetric());
		} catch (Exception e) {
			log.error("Error writing to InfluxDB", e);
		}
	}

	public void writeBatch(List<TimeSeriesDataPoint> dataPoints) {
		try {
			List<Point> points = dataPoints.stream()
					.map(dp -> {
						Point point = Point.measurement(dp.getMetric())
								.time(dp.getTimestamp(), WritePrecision.MS)
								.addField("value", dp.getValue().doubleValue());

						if (dp.getTags() != null) {
							dp.getTags().forEach(point::addTag);
						}

						return point;
					})
					.toList();

			writeApi.writePoints(points);
			log.debug("Written {} data points", points.size());
		} catch (Exception e) {
			log.error("Error writing batch to InfluxDB", e);
		}
	}

	public List<TimeSeriesDataPoint> query(String flux) {
		try {
			List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, organization);
			List<TimeSeriesDataPoint> results = new ArrayList<>();

			tables.forEach(table -> 
			table.getRecords().forEach(record -> {
				Map<String, String> tags = record.getValues().entrySet().stream()
						.filter(e -> e.getKey().startsWith("_") == false && e.getKey() != "value" && e.getKey() != "time" && e.getKey() != "measurement")
						.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
				
				TimeSeriesDataPoint point = TimeSeriesDataPoint.builder()
						.metric(record.getMeasurement())
						.value(new java.math.BigDecimal(record.getValue().toString()))
						.timestamp((Instant) record.getTime())
						.tags(tags)
						.build();
				results.add(point);
			})
					);

			return results;
		} catch (Exception e) {
			log.error("Error querying InfluxDB", e);
			return new ArrayList<>();
		}
	}

	public List<TimeSeriesDataPoint> getRecentMetrics(String measurement, Duration duration) {
		String flux = String.format(
				"from(bucket: \"%s\") " +
						"|> range(start: -%s) " +
						"|> filter(fn: (r) => r._measurement == \"%s\")",
						bucket, duration.toString(), measurement
				);

		return query(flux);
	}
	
}