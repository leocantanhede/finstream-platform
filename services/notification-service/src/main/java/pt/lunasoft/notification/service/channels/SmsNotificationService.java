package pt.lunasoft.notification.service.channels;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.exception.NotificationSendException;

@Service
@Slf4j
public class SmsNotificationService {

    @Value("${app.notification.sms.enabled}")
    private boolean enabled;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        if (enabled && accountSid != null && !accountSid.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized");
        }
    }

    public void send(NotificationEntity notification) {
        if (!enabled) {
            log.info("SMS notifications are disabled");
            return;
        }

        try {
            String toPhoneNumber = getRecipientPhoneNumber(notification);
            
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    notification.getContent()
            ).create();

            log.info("SMS sent successfully. SID: {} to: {}", 
                    message.getSid(), 
                    notification.getRecipientId());
            
        } catch (Exception e) {
            log.error("Failed to send SMS notification", e);
            throw new NotificationSendException("Failed to send SMS", e);
        }
    }

    private String getRecipientPhoneNumber(NotificationEntity notification) {
        if (notification.getMetadata() != null && 
            notification.getMetadata().containsKey("phoneNumber")) {
            return (String) notification.getMetadata().get("phoneNumber");
        }
        throw new IllegalArgumentException("Phone number not found in notification metadata");
    }
    
}