package pt.lunasoft.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.analytics.model.AccountMetrics;
import pt.lunasoft.analytics.model.RealTimeMetrics;
import pt.lunasoft.analytics.model.TimeSeriesDataPoint;
import pt.lunasoft.analytics.service.AnalyticsQueryService;
import pt.lunasoft.analytics.service.MetricsAggregationService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Analytics and metrics APIs")
public class AnalyticsController {

    private final MetricsAggregationService metricsAggregationService;
    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping("/metrics/realtime")
    @Operation(summary = "Get real-time global metrics")
    public Mono<RealTimeMetrics> getRealTimeMetrics() {
        return Mono.just(metricsAggregationService.getGlobalMetrics());
    }

    @GetMapping("/metrics/account/{accountId}")
    @Operation(summary = "Get metrics for specific account")
    public Mono<AccountMetrics> getAccountMetrics(@PathVariable String accountId) {
        return metricsAggregationService.getAccountMetrics(accountId);
    }

    @GetMapping("/timeseries/volume")
    @Operation(summary = "Get transaction volume time series")
    public Mono<List<TimeSeriesDataPoint>> getVolumeTimeSeries(
            @RequestParam(defaultValue = "1") int hours) {
        return Mono.just(analyticsQueryService.getTransactionVolumeTimeSeries(Duration.ofHours(hours)));
    }

    @GetMapping("/timeseries/count")
    @Operation(summary = "Get transaction count time series")
    public Mono<List<TimeSeriesDataPoint>> getCountTimeSeries(
            @RequestParam(defaultValue = "1") int hours) {
        return Mono.just(analyticsQueryService.getTransactionCountTimeSeries(Duration.ofHours(hours)));
    }

    @GetMapping("/distribution/category")
    @Operation(summary = "Get transaction distribution by category")
    public Mono<Map<String, Long>> getTransactionsByCategory(
            @RequestParam(defaultValue = "24") int hours) {
        return Mono.just(analyticsQueryService.getTransactionsByCategory(Duration.ofHours(hours)));
    }

    @GetMapping("/average/amount")
    @Operation(summary = "Get average transaction amount over time")
    public Mono<List<TimeSeriesDataPoint>> getAverageAmount(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "merchant_category") String groupBy) {
        return Mono.just(analyticsQueryService.getAverageTransactionAmount(Duration.ofHours(hours), groupBy));
    }
    
}