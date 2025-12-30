package pt.lunasoft.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pt.lunasoft.notification.entity.NotificationPreferenceEntity;
import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationType;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {

    List<NotificationPreferenceEntity> findByAccountId(String accountId);

    Optional<NotificationPreferenceEntity> findByAccountIdAndNotificationTypeAndChannel(String accountId, NotificationType notificationType, NotificationChannel channel);

    List<NotificationPreferenceEntity> findByAccountIdAndNotificationTypeAndEnabledTrue(String accountId, NotificationType notificationType);
    
}