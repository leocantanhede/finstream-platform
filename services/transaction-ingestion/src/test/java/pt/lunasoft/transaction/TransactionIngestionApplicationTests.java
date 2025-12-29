package pt.lunasoft.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.TransactionType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"transactions.incoming"})
@DirtiesContext
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class TransactionIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer(DockerImageName.parse("mysql:9.5.1"))
            .withDatabaseName("FINSTREAM_TEST")
            .withUsername("root")
            .withPassword("lun@2404");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTransaction() throws Exception {
        Transaction transaction = Transaction.builder()
                .accountId("ACC12345")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .type(TransactionType.PURCHASE)
                .merchant("Test Merchant")
                .timestamp(Instant.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.accountId").value("ACC12345"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldValidateTransactionInput() throws Exception {
        Transaction invalidTransaction = Transaction.builder()
                .amount(new BigDecimal("-10"))
                .currency("US") // Invalid - should be 3 chars
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }
}

