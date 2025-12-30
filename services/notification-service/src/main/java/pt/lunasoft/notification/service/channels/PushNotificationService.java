package pt.lunasoft.notification.service.channels;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidConfig.Priority;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.enums.NotificationPriority;
import pt.lunasoft.notification.exception.NotificationSendException;

@Service
@Slf4j
public class PushNotificationService {

	@Value("${app.notification.push.enabled}")
	private boolean enabled;

	public void send(NotificationEntity notification) {
		if (!enabled) {
			log.info("Push notifications are disabled");
			return;
		}

		try {
			String deviceToken = getDeviceToken(notification);

			com.google.firebase.messaging.Notification firebaseNotification = 
					com.google.firebase.messaging.Notification.builder()
					.setTitle(notification.getSubject())
					.setBody(notification.getContent())
					.build();

			Message message = Message.builder()
					.setToken(deviceToken)
					.setNotification(firebaseNotification)
					.putAllData(convertMetadata(notification.getMetadata()))
					.setAndroidConfig(AndroidConfig.builder()
							.setPriority(getPriority(notification.getPriority()))
							.build())
					.setApnsConfig(ApnsConfig.builder()
							.setAps(Aps.builder()
									.setSound("default")
									.build())
							.build())
					.build();
			String response = FirebaseMessaging.getInstance().send(message);
			log.info("Push notification sent successfully. Response: {}", response);

		} catch (Exception e) {
			log.error("Failed to send push notification", e);
			throw new NotificationSendException("Failed to send push notification", e);
		}
	}

	private String getDeviceToken(NotificationEntity notification) {
		if (notification.getMetadata() != null && 
				notification.getMetadata().containsKey("deviceToken")) {
			return (String) notification.getMetadata().get("deviceToken");
		}
		throw new IllegalArgumentException("Device token not found in notification metadata");
	}

	private Priority getPriority(NotificationPriority priority) {
		return switch (priority) {
		case URGENT, HIGH -> Priority.HIGH;
		default -> Priority.NORMAL;
		};
	}

	private Map<String, String> convertMetadata(Map<String, Object> metadata) {
		if (metadata == null) {
			return Map.of();
		}
		return metadata.entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						e -> String.valueOf(e.getValue())
						));
	}

}