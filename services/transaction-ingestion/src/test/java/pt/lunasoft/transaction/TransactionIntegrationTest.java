package pt.lunasoft.transaction;

import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.TransactionType;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"transactions.incoming"})
@DirtiesContext
class TransactionIntegrationTest {

    @SuppressWarnings("resource")
	@Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.5")
            .withDatabaseName("FINSTREAM_TEST")
            .withUsername("root")
            .withPassword("lun@2404")
            .withCommand("--default-authentication-plugin=mysql_native_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure MySQL datasource
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // Configure Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
        
        // Disable Liquibase for tests
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTransaction() throws Exception {
        // Given
        Transaction transaction = Transaction.builder()
                .accountId("ACC12345")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.accountId").value("ACC12345"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldValidateTransactionInput() throws Exception {
        // Given - Invalid transaction (negative amount and invalid currency)
        Transaction invalidTransaction = Transaction.builder()
                .accountId("ACC001")
                .amount(new BigDecimal("-10"))
                .currency("US") // Invalid - should be 3 chars
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetTransactionById() throws Exception {
        // Given - Create a transaction first
        Transaction transaction = Transaction.builder()
                .accountId("ACC12345")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        String response = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Transaction createdTransaction = objectMapper.readValue(response, Transaction.class);

        // When & Then - Retrieve the transaction
        mockMvc.perform(get("/api/v1/transactions/" + createdTransaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTransaction.getId().toString()))
                .andExpect(jsonPath("$.accountId").value("ACC12345"));
    }

    @Test
    void shouldGetTransactionsByAccount() throws Exception {
        // Given - Create multiple transactions
        for (int i = 0; i < 3; i++) {
            Transaction transaction = Transaction.builder()
                    .accountId("ACC999")
                    .amount(new BigDecimal("50.00"))
                    .currency("USD")
                    .type(TransactionType.PURCHASE)
                    .merchant("Test Merchant " + i)
                    .timestamp(Instant.now())
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transaction)))
                    .andExpect(status().isCreated());
        }

        // When & Then - Retrieve transactions by account
        mockMvc.perform(get("/api/v1/transactions/account/ACC999")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void shouldUpdateTransactionStatus() throws Exception {
        // Given - Create a transaction
        Transaction transaction = Transaction.builder()
                .accountId("ACC12345")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        String response = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Transaction createdTransaction = objectMapper.readValue(response, Transaction.class);

        // When & Then - Update status
        mockMvc.perform(patch("/api/v1/transactions/" + createdTransaction.getId() + "/status")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentTransaction() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/transactions/" + java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireAllMandatoryFields() throws Exception {
        // Given - Transaction without required fields
        Transaction invalidTransaction = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                // Missing accountId, currency, type, merchant, timestamp
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }
}