package pt.lunasoft.analytics.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.analytics.service.MetricsAggregationService;
import pt.lunasoft.models.FraudAlert;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAlertListener {

    private final MetricsAggregationService metricsAggregationService;

    @KafkaListener(topics = "${app.kafka.topics.fraud-alerts}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFraudAlert(@Payload FraudAlert alert) {
        log.info("Received fraud alert: {} for account: {}", alert.getId(), alert.getAccountId());
        metricsAggregationService.incrementFraudCount();
    }
    
}