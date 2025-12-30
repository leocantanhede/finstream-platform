package pt.lunasoft.notification.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

	private String templateId;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String bodyTemplate;
    private Map<String, Object> defaultVariables;
	
}