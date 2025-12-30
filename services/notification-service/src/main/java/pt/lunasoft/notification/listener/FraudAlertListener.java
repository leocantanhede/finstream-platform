package pt.lunasoft.notification.listener;

import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.models.FraudAlert;
import pt.lunasoft.models.enums.FraudSeverity;
import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationPriority;
import pt.lunasoft.notification.enums.NotificationType;
import pt.lunasoft.notification.model.Notification;
import pt.lunasoft.notification.service.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAlertListener {

	private final NotificationService notificationService;

	@KafkaListener(
			topics = "${app.kafka.topics.fraud-alerts}",
			groupId = "${spring.kafka.consumer.group-id}"
			)
	public void consumeFraudAlert(
			@Payload FraudAlert alert,
			Acknowledgment acknowledgment) {

		try {
			log.info("Received fraud alert: {} for account: {}", 
					alert.getId(), alert.getAccountId());

			// Send multiple notifications for critical fraud alerts
			if (alert.getSeverity() == FraudSeverity.CRITICAL || alert.getSeverity() == FraudSeverity.HIGH) {

				// Email notification
				sendEmailNotification(alert);

				// SMS notification for critical alerts
				if (alert.getSeverity() == FraudSeverity.CRITICAL) {
					sendSmsNotification(alert);
				}

				// Push notification
				sendPushNotification(alert);

				// Slack notification for internal monitoring
				sendSlackNotification(alert);
			} else {
				// For low/medium severity, just send email
				sendEmailNotification(alert);
			}

			acknowledgment.acknowledge();

		} catch (Exception e) {
			log.error("Error processing fraud alert: {}", alert.getId(), e);
			// Don't acknowledge - message will be reprocessed
		}
	}

	private void sendEmailNotification(FraudAlert alert) {
		Notification notification = Notification.builder()
				.id(UUID.randomUUID())
				.recipientId(alert.getAccountId())
				.type(NotificationType.FRAUD_ALERT)
				.channel(NotificationChannel.EMAIL)
				.subject("URGENT: Potential Fraudulent Activity Detected")
				.content(buildFraudAlertContent(alert))
				.metadata(Map.of(
						"alertId", alert.getId().toString(),
						"transactionId", alert.getTransactionId().toString(),
						"severity", alert.getSeverity().toString(),
						"riskScore", alert.getRiskScore().toString()
						))
				.priority(mapSeverityToPriority(alert.getSeverity()))
				.build();

		notificationService.sendNotification(notification);
	}

	private void sendSmsNotification(FraudAlert alert) {
		Notification notification = Notification.builder()
				.id(UUID.randomUUID())
				.recipientId(alert.getAccountId())
				.type(NotificationType.FRAUD_ALERT)
				.channel(NotificationChannel.SMS)
				.content(String.format(
						"ALERT: Suspicious activity detected on your account. " +
								"Transaction flagged with risk score %.2f. Please review immediately.",
								alert.getRiskScore()
						))
				.metadata(Map.of(
						"alertId", alert.getId().toString()
						))
				.priority(NotificationPriority.URGENT)
				.build();

		notificationService.sendNotification(notification);
	}

	private void sendPushNotification(FraudAlert alert) {
		Notification notification = Notification.builder()
				.id(UUID.randomUUID())
				.recipientId(alert.getAccountId())
				.type(NotificationType.FRAUD_ALERT)
				.channel(NotificationChannel.PUSH)
				.subject("Security Alert")
				.content("Suspicious activity detected on your account. Tap to review.")
				.metadata(Map.of(
						"alertId", alert.getId().toString(),
						"action", "open_alert",
						"alertType", "fraud"
						))
				.priority(NotificationPriority.URGENT)
				.build();

		notificationService.sendNotification(notification);
	}

	private void sendSlackNotification(FraudAlert alert) {
		Notification notification = Notification.builder()
				.id(UUID.randomUUID())
				.recipientId("internal-security-team")
				.type(NotificationType.FRAUD_ALERT)
				.channel(NotificationChannel.SLACK)
				.subject("Fraud Alert Triggered")
				.content(buildSlackAlertContent(alert))
				.priority(NotificationPriority.HIGH)
				.build();

		notificationService.sendNotification(notification);
	}

	private String buildFraudAlertContent(FraudAlert alert) {
		return String.format(
				"We've detected potentially fraudulent activity on your account.\n\n" +
						"Details:\n" +
						"- Alert ID: %s\n" +
						"- Transaction ID: %s\n" +
						"- Risk Score: %.2f\n" +
						"- Severity: %s\n" +
						"- Triggered Rules: %s\n\n" +
						"If you did not authorize this transaction, please contact us immediately.\n" +
						"Otherwise, you can mark this alert as a false positive in your account dashboard.",
						alert.getId(),
						alert.getTransactionId(),
						alert.getRiskScore(),
						alert.getSeverity(),
						String.join(", ", alert.getTriggeredRules())
				);
	}

	private String buildSlackAlertContent(FraudAlert alert) {
		return String.format(
				"🚨 *Fraud Alert*\n" +
						"Account: `%s`\n" +
						"Severity: *%s*\n" +
						"Risk Score: %.2f\n" +
						"Rules: %s",
						alert.getAccountId(),
						alert.getSeverity(),
						alert.getRiskScore(),
						String.join(", ", alert.getTriggeredRules())
				);
	}

	private NotificationPriority mapSeverityToPriority(FraudSeverity severity) {
		return switch (severity) {
		case CRITICAL -> NotificationPriority.URGENT;
		case HIGH -> NotificationPriority.HIGH;
		case MEDIUM -> NotificationPriority.MEDIUM;
		case LOW -> NotificationPriority.LOW;
		};
	}
	
}