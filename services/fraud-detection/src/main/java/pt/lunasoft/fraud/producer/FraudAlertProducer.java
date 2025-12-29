package pt.lunasoft.fraud.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.models.FraudAlert;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertProducer {

	private final KafkaTemplate<String, FraudAlert> kafkaTemplate;

	@Value("${app.kafka.topics.fraud-alerts}")
	private String fraudAlertsTopic;

	public void sendAlert(FraudAlert alert) {
		log.info("Sending fraud alert: {} for account: {}", alert.getId(), alert.getAccountId());
		kafkaTemplate.send(fraudAlertsTopic, alert.getAccountId(), alert).whenComplete((result, ex) -> {
			if (ex == null) {
				log.info("Alert sent successfully: {}", alert.getId());
			} else {
				log.error("Failed to send alert: {}", alert.getId(), ex);
			}
		});
		
	}

}