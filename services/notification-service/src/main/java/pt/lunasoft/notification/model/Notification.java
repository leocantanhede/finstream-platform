package pt.lunasoft.notification.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationPriority;
import pt.lunasoft.notification.enums.NotificationStatus;
import pt.lunasoft.notification.enums.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

	private UUID id;
    private String recipientId;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String content;
    private Map<String, Object> metadata;
    private NotificationPriority priority;
    private NotificationStatus status;
    private Instant createdAt;
    private Instant sentAt;
    private String errorMessage;
    private Integer retryCount;
	
}