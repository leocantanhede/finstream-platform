package pt.lunasoft.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientId(String recipientId, Pageable pageable);

    List<NotificationEntity> findByStatus(NotificationStatus status);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = :status AND n.retryCount < :maxRetries AND n.createdAt > :since")
    List<NotificationEntity> findPendingRetries(@Param("status") NotificationStatus status, @Param("maxRetries") int maxRetries, @Param("since") Instant since);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.recipientId = :recipientId AND n.channel = :channel AND n.createdAt >= :since")
    long countRecentNotifications(@Param("recipientId") String recipientId, @Param("channel") NotificationChannel channel, @Param("since") Instant since);
    
}