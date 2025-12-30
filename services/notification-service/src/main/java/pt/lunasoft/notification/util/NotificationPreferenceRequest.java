package pt.lunasoft.notification.util;

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
public class NotificationPreferenceRequest {

	private String accountId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private Boolean enabled;
    private String destination;
	
}