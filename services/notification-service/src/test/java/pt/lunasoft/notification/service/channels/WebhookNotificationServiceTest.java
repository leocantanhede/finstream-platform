package pt.lunasoft.notification.service.channels;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationPriority;
import pt.lunasoft.notification.enums.NotificationStatus;
import pt.lunasoft.notification.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
class WebhookNotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookNotificationService webhookService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookService, "enabled", true);
        ReflectionTestUtils.setField(webhookService, "webhookSecretKey", "test-secret-key");
    }

    @Test
    void shouldSendWebhookSuccessfully() throws Exception {
        // Given
        NotificationEntity notification = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .recipientId("user123")
                .type(NotificationType.FRAUD_ALERT)
                .channel(NotificationChannel.WEBHOOK)
                .subject("Test Alert")
                .content("Test content")
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.PENDING)
                .metadata(Map.of("webhookUrl", "https://example.com/webhook"))
                .build();

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"test\":\"payload\"}");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // When
        webhookService.send(notification);

        // Then
        verify(restTemplate).exchange(
                eq("https://example.com/webhook"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void shouldIncludeSignatureInHeaders() throws Exception {
        // Given
        NotificationEntity notification = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .recipientId("user123")
                .type(NotificationType.FRAUD_ALERT)
                .channel(NotificationChannel.WEBHOOK)
                .metadata(Map.of("webhookUrl", "https://example.com/webhook"))
                .build();

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"test\":\"payload\"}");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // When
        webhookService.send(notification);

        // Then
        HttpEntity<String> capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getHeaders().containsKey("X-Webhook-Signature")).isTrue();
        assertThat(capturedRequest.getHeaders().containsKey("X-Notification-ID")).isTrue();
    }
    
}