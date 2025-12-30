package pt.lunasoft.notification.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationPriority;
import pt.lunasoft.notification.enums.NotificationStatus;
import pt.lunasoft.notification.enums.NotificationType;

@Entity
@Table(name = "NOTIFICATION", indexes = {
        @Index(name = "IDX_RECIPIENT_ID", columnList = "RECIPIENT_ID"),
        @Index(name = "IDX_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_CREATED_AT", columnList = "CREATED_AT"),
        @Index(name = "IDX_TYPE", columnList = "TYPE")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "RECIPIENT_ID", nullable = false, length = 100)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "SUBJECT", length = 500)
    private String subject;

    @Column(name = "CONTENT", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "METADATA", columnDefinition = "JSON")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORITY", nullable = false, length = 20)
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private NotificationStatus status;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "SENT_AT")
    private Instant sentAt;

    @Column(name = "DELIVERED_AT")
    private Instant deliveredAt;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "RETRY_COUNT")
    private Integer retryCount;

    @Column(name = "EXTERNAL_ID", length = 255)
    private String externalId;
    
}