package pt.lunasoft.analytics.controller;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.analytics.model.RealTimeMetrics;
import pt.lunasoft.analytics.service.MetricsAggregationService;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/analytics/stream")
@RequiredArgsConstructor
@Slf4j
public class StreamingAnalyticsController {

	private final MetricsAggregationService metricsAggregationService;

	@GetMapping(value = "/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream real-time metrics via Server-Sent Events")
	public Flux<RealTimeMetrics> streamRealTimeMetrics() {
		return Flux.interval(Duration.ofSeconds(1))
				.map(tick -> metricsAggregationService.getGlobalMetrics())
				.doOnNext(metrics -> log.debug("Streaming metrics: {} TPS", metrics.getTransactionsPerSecond()));
	}
	
}