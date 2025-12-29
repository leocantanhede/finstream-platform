package pt.lunasoft.fraud;

import pt.lunasoft.fraud.service.FraudDetectionService;

import pt.lunasoft.models.enums.TransactionType;
import pt.lunasoft.models.DeviceInfo;
import pt.lunasoft.models.FraudAlert;
import pt.lunasoft.models.Location;
import pt.lunasoft.models.Transaction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"fraud.alerts"})
@DirtiesContext
class FraudDetectionIntegrationTest {

    @SuppressWarnings("resource")
	@Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.5")
            .withDatabaseName("finstream_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--default-authentication-plugin=mysql_native_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Test
    void shouldDetectHighAmountFraud() {
        // Given
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId("ACC001")
                .amount(new BigDecimal("15000")) // High amount
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        // When
        FraudAlert alert = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(alert).isNotNull();
        assertThat(alert.getRiskScore()).isGreaterThan(0);
        assertThat(alert.getTriggeredRules()).contains("HIGH_AMOUNT");
    }

    @Test
    void shouldNotDetectFraudForNormalTransaction() {
        // Given
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId("ACC002")
                .amount(new BigDecimal("50.00")) // Normal amount
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .location(Location.builder()
                        .country("US")
                        .latitude(40.7128)
                        .longitude(-74.0060)
                        .build())
                .deviceInfo(DeviceInfo.builder()
                        .deviceId("device-123")
                        .deviceType("mobile")
                        .build())
                .build();

        // When
        FraudAlert alert = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(alert).isNull();
    }

    @Test
    void shouldDetectRapidSuccessionFraud() {
        // Given
        String accountId = "ACC003";
        
        // First transaction
        Transaction transaction1 = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now().minusSeconds(10))
                .build();
        
        fraudDetectionService.analyzeTransaction(transaction1);

        // Second transaction immediately after
        Transaction transaction2 = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        // When
        FraudAlert alert = fraudDetectionService.analyzeTransaction(transaction2);

        // Then
        assertThat(alert).isNotNull();
        assertThat(alert.getTriggeredRules()).contains("RAPID_SUCCESSION");
    }
}