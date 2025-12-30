package pt.lunasoft.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

import pt.lunasoft.analytics.model.TransactionStats;
import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.TransactionType;

class TransactionAnalyticsTopologyTest {

	private TopologyTestDriver testDriver;
    private TestInputTopic<String, Transaction> inputTopic;
    private TestOutputTopic<String, TransactionStats> outputTopic;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);

        StreamsBuilder builder = new StreamsBuilder();
        // Build topology here

        testDriver = new TopologyTestDriver(builder.build(), props);

        try (JsonSerde<Transaction> transaction = new JsonSerde<>(Transaction.class)) {
			inputTopic = testDriver.createInputTopic("transactions.incoming", Serdes.String().serializer(), transaction.serializer());
		}
        
		try (JsonSerde<TransactionStats> transactionStats = new JsonSerde<>(TransactionStats.class)) {
			outputTopic = testDriver.createOutputTopic("analytics.aggregated.by-account", Serdes.String().deserializer(), transactionStats.deserializer());
		}
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void shouldAggregateTransactionsByAccount() {
        // Given
        Transaction tx1 = createTransaction("ACC001", new BigDecimal("100"));
        Transaction tx2 = createTransaction("ACC001", new BigDecimal("200"));

        // When
        inputTopic.pipeInput("ACC001", tx1);
        inputTopic.pipeInput("ACC001", tx2);

        // Then
        TransactionStats stats = outputTopic.readValue();
        assertThat(stats.getTransactionCount()).isEqualTo(2);
        assertThat(stats.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300"));
    }

    private Transaction createTransaction(String accountId, BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .amount(amount)
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();
    }

}