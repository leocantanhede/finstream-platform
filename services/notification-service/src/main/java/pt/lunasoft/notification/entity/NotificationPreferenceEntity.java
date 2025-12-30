package pt.lunasoft.notification.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationType;

@Entity
@Table(name = "NOTIFICATION_PREFERENCE", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"ACCOUNT_ID", "NOTIFICATION_TYPE", "CHANNEL"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "ACCOUNT_ID", nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_TYPE", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false)
    private NotificationChannel channel;

    @Column(name = "ENABLED", nullable = false)
    private Boolean enabled;

    @Column(name = "DESTINATION", length = 255)
    private String destination; // Email address, phone number, etc.
    
}
