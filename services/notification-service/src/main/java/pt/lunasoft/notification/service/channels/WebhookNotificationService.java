package pt.lunasoft.notification.service.channels;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.exception.NotificationSendException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.notification.webhook.enabled}")
	private boolean enabled;
	
	@Value("${app.notification.webhook.secret-key}")
	private String webhookSecretKey;

	@Retryable(
			maxAttempts = 3,
			backoff = @Backoff(delay = 1000, multiplier = 2)
			)
	public void send(NotificationEntity notification) {
		if (!enabled) {
			log.info("Webhook notifications are disabled");
			return;
		}

		try {
			String webhookUrl = getWebhookUrl(notification);

			// Build payload
			Map<String, Object> payload = buildPayload(notification);

			// Serialize payload to JSON using ObjectMapper
			String jsonPayload;
			try {
				jsonPayload = objectMapper.writeValueAsString(payload);
				log.debug("Webhook payload: {}", jsonPayload);
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize webhook payload", e);
				throw new NotificationSendException("Failed to serialize payload", e);
			}

			// Prepare headers
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("X-Notification-ID", notification.getId().toString());
			headers.set("X-Notification-Type", notification.getType().toString());
			headers.set("X-Notification-Priority", notification.getPriority().toString());
			headers.set("X-Webhook-Signature", generateSignature(jsonPayload));
			headers.set("X-Timestamp", Instant.now().toString());
			headers.set("User-Agent", "FinStream-Notification-Service/1.0");

			// Create request with JSON string body
			HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

			// Send request
			ResponseEntity<String> response = restTemplate.exchange(
					webhookUrl,
					HttpMethod.POST,
					request,
					String.class
					);

			// Validate response
			if (response.getStatusCode().is2xxSuccessful()) {
				log.info("Webhook notification sent successfully to: {} - Response: {}", 
						webhookUrl, response.getStatusCode());

				// Log response body if present
				if (response.hasBody()) {
					log.debug("Webhook response body: {}", response.getBody());
				}
			} else {
				log.warn("Webhook returned non-2xx status: {} - Body: {}", 
						response.getStatusCode(), response.getBody());
				throw new NotificationSendException(
						"Webhook returned non-2xx status: " + response.getStatusCode());
			}

		} catch (Exception e) {
			log.error("Failed to send webhook notification to: {}", 
					notification.getMetadata() != null ? 
							notification.getMetadata().get("webhookUrl") : "unknown", 
							e);
			throw new NotificationSendException("Failed to send webhook", e);
		}
	}

	/**
	 * Build the webhook payload with notification details
	 */
	private Map<String, Object> buildPayload(NotificationEntity notification) {
		Map<String, Object> payload = new HashMap<>();

		// Core notification data
		payload.put("id", notification.getId().toString());
		payload.put("type", notification.getType().toString());
		payload.put("channel", notification.getChannel().toString());
		payload.put("recipientId", notification.getRecipientId());
		payload.put("priority", notification.getPriority().toString());
		payload.put("status", notification.getStatus().toString());

		// Content
		if (notification.getSubject() != null) {
			payload.put("subject", notification.getSubject());
		}
		if (notification.getContent() != null) {
			payload.put("content", notification.getContent());
		}

		// Timestamps
		payload.put("createdAt", notification.getCreatedAt().toString());
		payload.put("timestamp", Instant.now().toString());

		// Metadata
		if (notification.getMetadata() != null && !notification.getMetadata().isEmpty()) {
			// Create a copy without sensitive data
			Map<String, Object> sanitizedMetadata = new HashMap<>(notification.getMetadata());
			sanitizedMetadata.remove("webhookUrl"); // Don't send webhook URL back
			sanitizedMetadata.remove("apiKey"); // Don't send API keys
			sanitizedMetadata.remove("secret"); // Don't send secrets

			payload.put("metadata", sanitizedMetadata);
		}

		// Add event type for webhook consumers
		payload.put("event", "notification." + notification.getType().toString().toLowerCase());

		return payload;
	}

	/**
	 * Get webhook URL from notification metadata
	 */
	private String getWebhookUrl(NotificationEntity notification) {
		if (notification.getMetadata() != null && 
				notification.getMetadata().containsKey("webhookUrl")) {

			String url = (String) notification.getMetadata().get("webhookUrl");

			// Validate URL format
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				throw new IllegalArgumentException("Invalid webhook URL format: " + url);
			}

			return url;
		}

		throw new IllegalArgumentException("Webhook URL not found in notification metadata");
	}

	/**
	 * Generate HMAC signature for webhook payload
	 * This allows webhook consumers to verify authenticity
	 */
	private String generateSignature(String payload) {
	    try {
	        Mac mac = Mac.getInstance("HmacSHA256");
	        SecretKeySpec secretKeySpec = 
	                new SecretKeySpec(
	                        webhookSecretKey.getBytes(StandardCharsets.UTF_8), 
	                        "HmacSHA256"
	                );
	        mac.init(secretKeySpec);
	        
	        byte[] signatureBytes = mac.doFinal(
	                payload.getBytes(StandardCharsets.UTF_8)
	        );
	        
	        // Convert to hex string
	        StringBuilder hexString = new StringBuilder();
	        for (byte b : signatureBytes) {
	            String hex = Integer.toHexString(0xff & b);
	            if (hex.length() == 1) {
	                hexString.append('0');
	            }
	            hexString.append(hex);
	        }
	        
	        return hexString.toString();
	        
	    } catch (Exception e) {
	        log.error("Failed to generate webhook signature", e);
	        return "";
	    }
	}

	/**
	 * Send a test webhook to verify connectivity
	 */
	public boolean testWebhook(String webhookUrl) {
		try {
			Map<String, Object> testPayload = new HashMap<>();
			testPayload.put("event", "webhook.test");
			testPayload.put("message", "This is a test webhook from FinStream");
			testPayload.put("timestamp", Instant.now().toString());

			String jsonPayload = objectMapper.writeValueAsString(testPayload);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("X-Webhook-Test", "true");

			HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

			ResponseEntity<String> response = restTemplate.exchange(
					webhookUrl,
					HttpMethod.POST,
					request,
					String.class
					);

			boolean success = response.getStatusCode().is2xxSuccessful();
			log.info("Webhook test to {} result: {}", webhookUrl, success);

			return success;

		} catch (Exception e) {
			log.error("Webhook test failed for {}", webhookUrl, e);
			return false;
		}
	}
	
}