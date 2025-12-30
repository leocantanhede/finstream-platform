package pt.lunasoft.notification.service.channels;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.exception.NotificationSendException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.notification.email.enabled}")
    private boolean enabled;

    @Value("${app.notification.email.from}")
    private String fromAddress;

    public void send(NotificationEntity notification) {
        if (!enabled) {
            log.info("Email notifications are disabled");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(getRecipientEmail(notification));
            helper.setSubject(notification.getSubject());
            
            // Generate HTML content from template
            String htmlContent = generateHtmlContent(notification);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", notification.getRecipientId());
            
        } catch (MessagingException e) {
            log.error("Failed to send email notification", e);
            throw new NotificationSendException("Failed to send email", e);
        }
    }

    private String getRecipientEmail(NotificationEntity notification) {
        // In real implementation, fetch from user service or notification metadata
        return notification.getMetadata() != null ? 
                (String) notification.getMetadata().get("email") : 
                notification.getRecipientId() + "@example.com";
    }

    private String generateHtmlContent(NotificationEntity notification) {
        Context context = new Context();
        context.setVariable("subject", notification.getSubject());
        context.setVariable("content", notification.getContent());
        context.setVariable("metadata", notification.getMetadata());
        context.setVariable("type", notification.getType());

        // Use appropriate template based on notification type
        String template = getTemplateForType(notification.getType().toString());
        
        return templateEngine.process(template, context);
    }

    private String getTemplateForType(String type) {
        return switch (type) {
            case "FRAUD_ALERT" -> "email/fraud-alert";
            case "TRANSACTION_CONFIRMATION" -> "email/transaction-confirmation";
            case "ACCOUNT_LOCKED" -> "email/account-locked";
            default -> "email/default";
        };
    }
    
}