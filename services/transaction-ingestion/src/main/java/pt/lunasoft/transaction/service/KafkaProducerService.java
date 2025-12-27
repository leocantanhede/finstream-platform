package pt.lunasoft.transaction.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.models.Transaction;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

	private final KafkaTemplate<String, Transaction> kafkaTemplate;

    @Value("${app.kafka.topics.transactions-incoming}")
    private String transactionsIncomingTopic;

    public void sendTransaction(Transaction transaction) {
        log.debug("Sending transaction to Kafka: {}", transaction.getId());
        
        CompletableFuture<SendResult<String, Transaction>> future = kafkaTemplate.send(transactionsIncomingTopic, transaction.getAccountId(), transaction);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction sent successfully: {} to partition: {}", transaction.getId(), result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send transaction: {}", transaction.getId(), ex);
            }
        });
    }
	
}