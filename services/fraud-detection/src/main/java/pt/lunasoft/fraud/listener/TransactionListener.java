package pt.lunasoft.fraud.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.fraud.producer.FraudAlertProducer;
import pt.lunasoft.fraud.service.FraudDetectionService;
import pt.lunasoft.fraud.service.TransactionHistoryService;
import pt.lunasoft.models.FraudAlert;
import pt.lunasoft.models.Transaction;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionListener {

	private final FraudDetectionService fraudDetectionService;
	private final TransactionHistoryService historyService;
	private final FraudAlertProducer alertProducer;

	@KafkaListener(
		topics = "${app.kafka.topics.transactions-incoming}",
		groupId = "${spring.kafka.consumer.group-id}",
		containerFactory = "kafkaListenerContainerFactory"
	)
	public void consumeTransaction(@Payload Transaction transaction, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, 
			@Header(KafkaHeaders.OFFSET) long offset, Acknowledgment acknowledgment) {
		try {
			log.info("Consumed transaction: {} from partition: {} offset: {}", transaction.getId(), partition, offset);

			// Add to history
			historyService.addTransaction(transaction);

			// Analyze for fraud
			FraudAlert alert = fraudDetectionService.analyzeTransaction(transaction);

			if (alert != null) {
				log.warn("Fraud detected! Alert: {} for transaction: {}", alert.getId(), transaction.getId());
				alertProducer.sendAlert(alert);
			} else {
				log.debug("Transaction clean: {}", transaction.getId());
			}

			// Manual commit
			acknowledgment.acknowledge();

		} catch (Exception e) {
			log.error("Error processing transaction: {}", transaction.getId(), e);
			// Don't acknowledge - message will be reprocessed
		}
	}

}