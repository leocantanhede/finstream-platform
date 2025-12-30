package pt.lunasoft.notification.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.service.channels.EmailNotificationService;
import pt.lunasoft.notification.service.channels.PushNotificationService;
import pt.lunasoft.notification.service.channels.SlackNotificationService;
import pt.lunasoft.notification.service.channels.SmsNotificationService;
import pt.lunasoft.notification.service.channels.WebhookNotificationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final PushNotificationService pushService;
    private final WebhookNotificationService webhookService;
    private final SlackNotificationService slackService;

    public void dispatch(NotificationEntity notification) {
        log.info("Dispatching notification: {} via {}", 
                notification.getId(), 
                notification.getChannel());

        switch (notification.getChannel()) {
            case EMAIL -> emailService.send(notification);
            case SMS -> smsService.send(notification);
            case PUSH -> pushService.send(notification);
            case WEBHOOK -> webhookService.send(notification);
            case SLACK -> slackService.send(notification);
            default -> log.warn("Unsupported notification channel: {}", notification.getChannel());
        }
    }
    
}