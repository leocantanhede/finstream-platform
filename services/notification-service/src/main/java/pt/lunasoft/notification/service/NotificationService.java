package pt.lunasoft.notification.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.enums.NotificationStatus;
import pt.lunasoft.notification.exception.NotificationNotFoundException;
import pt.lunasoft.notification.model.Notification;
import pt.lunasoft.notification.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationDispatcher notificationDispatcher;
	private final NotificationPreferenceService preferenceService;
	private final RateLimitService rateLimitService;

	@Async
	@Transactional
	public void sendNotification(Notification notification) {
		log.info("Processing notification: {} for recipient: {}", 
				notification.getType(), notification.getRecipientId());

		// Check if user has opted in for this notification type
		if (!preferenceService.isEnabled(
				notification.getRecipientId(), 
				notification.getType(), 
				notification.getChannel())) {
			log.info("Notification disabled by user preference: {} - {} - {}", 
					notification.getRecipientId(), 
					notification.getType(), 
					notification.getChannel());
			return;
		}

		// Check rate limits
		if (!rateLimitService.allowNotification(
				notification.getRecipientId(), 
				notification.getChannel())) {
			log.warn("Rate limit exceeded for: {} - {}", 
					notification.getRecipientId(), 
					notification.getChannel());
			saveNotification(notification, NotificationStatus.FAILED, 
					"Rate limit exceeded");
			return;
		}

		// Save notification
		NotificationEntity entity = saveNotification(
				notification, 
				NotificationStatus.PENDING, 
				null
				);

		// Dispatch notification
		try {
			notificationDispatcher.dispatch(entity);
			updateNotificationStatus(entity.getId(), NotificationStatus.SENT, null);
		} catch (Exception e) {
			log.error("Failed to send notification: {}", entity.getId(), e);
			updateNotificationStatus(entity.getId(), NotificationStatus.FAILED, e.getMessage());
		}
	}

	private NotificationEntity saveNotification(
			Notification notification, 
			NotificationStatus status, 
			String errorMessage) {

		NotificationEntity entity = NotificationEntity.builder()
				.id(notification.getId() != null ? notification.getId() : UUID.randomUUID())
				.recipientId(notification.getRecipientId())
				.type(notification.getType())
				.channel(notification.getChannel())
				.subject(notification.getSubject())
				.content(notification.getContent())
				.metadata(notification.getMetadata())
				.priority(notification.getPriority())
				.status(status)
				.retryCount(0)
				.errorMessage(errorMessage)
				.build();

		return notificationRepository.save(entity);
	}

	@Transactional
	public void updateNotificationStatus(UUID id, NotificationStatus status, String errorMessage) {
		notificationRepository.findById(id).ifPresent(entity -> {
			entity.setStatus(status);
			entity.setErrorMessage(errorMessage);

			if (status == NotificationStatus.SENT) {
				entity.setSentAt(Instant.now());
			} else if (status == NotificationStatus.DELIVERED) {
				entity.setDeliveredAt(Instant.now());
			}

			notificationRepository.save(entity);
		});
	}

	public Page<NotificationEntity> getNotificationsByRecipient(
			String recipientId, 
			Pageable pageable) {
		return notificationRepository.findByRecipientId(recipientId, pageable);
	}

	public NotificationEntity getNotificationById(UUID id) {
		return notificationRepository.findById(id).orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
	}

}