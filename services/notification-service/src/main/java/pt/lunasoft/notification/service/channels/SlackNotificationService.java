package pt.lunasoft.notification.service.channels;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;

import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.exception.NotificationSendException;

@Service
@Slf4j
public class SlackNotificationService {

	@Value("${app.notification.slack.enabled}")
	private boolean enabled;

	@Value("${app.notification.slack.webhook-url}")
	private String webhookUrl;

	private final Slack slack = Slack.getInstance();

	public void send(NotificationEntity notification) {
		if (!enabled) {
			log.info("Slack notifications are disabled");
			return;
		}

		try {
			String message = formatSlackMessage(notification);

			Payload payload = Payload.builder()
					.text(message)
					.build();

			WebhookResponse response = slack.send(webhookUrl, payload);

			if (response.getCode() == 200) {
				log.info("Slack notification sent successfully");
			} else {
				throw new NotificationSendException(
						"Slack webhook returned non-200: " + response.getCode());
			}

		} catch (IOException e) {
			log.error("Failed to send Slack notification", e);
			throw new NotificationSendException("Failed to send Slack notification", e);
		}
	}

	private String formatSlackMessage(NotificationEntity notification) {
		return String.format("*%s*\n%s\n_Type: %s | Priority: %s_",
				notification.getSubject(),
				notification.getContent(),
				notification.getType(),
				notification.getPriority()
				);
	}
	
}