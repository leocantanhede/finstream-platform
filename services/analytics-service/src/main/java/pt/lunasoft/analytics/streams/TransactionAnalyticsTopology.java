package pt.lunasoft.analytics.streams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.analytics.model.TransactionStats;
import pt.lunasoft.models.Transaction;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionAnalyticsTopology {

	@Value("${app.kafka.topics.transactions-incoming}")
	private String transactionsIncomingTopic;

	@Value("${app.kafka.topics.analytics-aggregated}")
	private String analyticsAggregatedTopic;

	@Value("${app.analytics.window.size-minutes}")
	private int windowSizeMinutes;

	@Value("${app.analytics.window.grace-period-minutes}")
	private int gracePeriodMinutes;

	private final JsonSerde<Transaction> transactionSerde;
	private final JsonSerde<TransactionStats> transactionStatsSerde;

	@Bean
	public KStream<String, Transaction> processTransactionStream(StreamsBuilder builder) {
		// Input stream
		KStream<String, Transaction> transactionStream = builder
				.stream(transactionsIncomingTopic, 
						Consumed.with(Serdes.String(), transactionSerde))
				.peek((key, value) -> log.debug("Processing transaction: {}", value.getId()));

		// Aggregate by account - Tumbling Window
		aggregateByAccount(transactionStream);

		// Aggregate by merchant category
		aggregateByMerchantCategory(transactionStream);

		// Global statistics
		calculateGlobalStats(transactionStream);

		return transactionStream;
	}

	private void aggregateByAccount(KStream<String, Transaction> stream) {
		TimeWindows tumblingWindow = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(windowSizeMinutes));

		stream
		.groupByKey(Grouped.with(Serdes.String(), transactionSerde))
		.windowedBy(tumblingWindow)
		.aggregate(this::initializeStats, this::aggregateTransaction, Materialized.with(Serdes.String(), transactionStatsSerde))
		.toStream()
		.map((windowedKey, stats) -> {
			stats.setWindowStart(Instant.ofEpochMilli(windowedKey.window().start()));
			stats.setWindowEnd(Instant.ofEpochMilli(windowedKey.window().end()));
			return KeyValue.pair(windowedKey.key(), stats);
		})
		.peek((key, value) -> log.info("Account aggregation: {} - {} transactions", key, value.getTransactionCount()))
		.to(analyticsAggregatedTopic + ".by-account", Produced.with(Serdes.String(), transactionStatsSerde));
	}

	private void aggregateByMerchantCategory(KStream<String, Transaction> stream) {
		TimeWindows tumblingWindow = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(windowSizeMinutes));

		stream
		.filter((key, transaction) -> transaction.getMerchantCategory() != null)
		.selectKey((key, transaction) -> transaction.getMerchantCategory())
		.groupByKey(Grouped.with(Serdes.String(), transactionSerde))
		.windowedBy(tumblingWindow)
		.aggregate(this::initializeStats, this::aggregateTransaction, Materialized.with(Serdes.String(), transactionStatsSerde))
		.toStream()
		.map((windowedKey, stats) -> {
			stats.setWindowStart(Instant.ofEpochMilli(windowedKey.window().start()));
			stats.setWindowEnd(Instant.ofEpochMilli(windowedKey.window().end()));
			stats.setMerchantCategory(windowedKey.key());
			return KeyValue.pair(windowedKey.key(), stats);
		})
		.peek((key, value) -> log.info("Category aggregation: {} - {} transactions", key, value.getTransactionCount()))
		.to(analyticsAggregatedTopic + ".by-category", Produced.with(Serdes.String(), transactionStatsSerde));
	}

	private void calculateGlobalStats(KStream<String, Transaction> stream) {
		stream
		.selectKey((key, value) -> "global")
		.groupByKey(Grouped.with(Serdes.String(), transactionSerde))
		.windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
		.aggregate(this::initializeStats, this::aggregateTransaction, Materialized.with(Serdes.String(), transactionStatsSerde))
		.toStream()
		.map((windowedKey, stats) -> {
			stats.setWindowStart(Instant.ofEpochMilli(windowedKey.window().start()));
			stats.setWindowEnd(Instant.ofEpochMilli(windowedKey.window().end()));
			return KeyValue.pair("global", stats);
		})
		.peek((key, value) -> log.info("Global stats: {} TPS", value.getTransactionCount() / windowSizeMinutes / 60.0))
		.to(analyticsAggregatedTopic + ".global", Produced.with(Serdes.String(), transactionStatsSerde));
	}

	private TransactionStats initializeStats() {
		return TransactionStats.builder()
				.transactionCount(0L)
				.totalAmount(BigDecimal.ZERO)
				.minAmount(null)
				.maxAmount(null)
				.build();
	}

	private TransactionStats aggregateTransaction(String key, Transaction transaction, TransactionStats stats) {
		BigDecimal amount = transaction.getAmount();

		stats.setAccountId(transaction.getAccountId());
		stats.setTransactionCount(stats.getTransactionCount() + 1);
		stats.setTotalAmount(stats.getTotalAmount().add(amount));

		// Update min/max
		if (stats.getMinAmount() == null || amount.compareTo(stats.getMinAmount()) < 0) {
			stats.setMinAmount(amount);
		}
		if (stats.getMaxAmount() == null || amount.compareTo(stats.getMaxAmount()) > 0) {
			stats.setMaxAmount(amount);
		}

		// Calculate average
		BigDecimal count = new BigDecimal(stats.getTransactionCount());
		stats.setAverageAmount(stats.getTotalAmount().divide(count, 2, RoundingMode.HALF_UP));

		return stats;
	}
	
}