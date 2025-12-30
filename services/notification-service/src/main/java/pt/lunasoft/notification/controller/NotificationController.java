package pt.lunasoft.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationEntity;
import pt.lunasoft.notification.entity.NotificationPreferenceEntity;
import pt.lunasoft.notification.model.Notification;
import pt.lunasoft.notification.service.NotificationPreferenceService;
import pt.lunasoft.notification.service.NotificationService;
import pt.lunasoft.notification.util.NotificationPreferenceRequest;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationPreferenceService preferenceService;

	@PostMapping
	@Operation(summary = "Send a notification")
	public ResponseEntity<Void> sendNotification(@RequestBody Notification notification) {
		log.info("Received request to send notification: {}", notification.getType());
		notificationService.sendNotification(notification);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get notification by ID")
	public ResponseEntity<NotificationEntity> getNotification(@PathVariable UUID id) {
		NotificationEntity notification = notificationService.getNotificationById(id);
		return ResponseEntity.ok(notification);
	}

	@GetMapping("/recipient/{recipientId}")
	@Operation(summary = "Get notifications for a recipient")
	public ResponseEntity<Page<NotificationEntity>> getNotificationsByRecipient(
			@PathVariable String recipientId,
			Pageable pageable) {
		Page<NotificationEntity> notifications = 
				notificationService.getNotificationsByRecipient(recipientId, pageable);
		return ResponseEntity.ok(notifications);
	}

	@GetMapping("/preferences/{accountId}")
	@Operation(summary = "Get notification preferences for an account")
	public ResponseEntity<List<NotificationPreferenceEntity>> getPreferences(
			@PathVariable String accountId) {
		List<NotificationPreferenceEntity> preferences = 
				preferenceService.getPreferences(accountId);
		return ResponseEntity.ok(preferences);
	}

	@PutMapping("/preferences")
	@Operation(summary = "Update notification preference")
	public ResponseEntity<Void> updatePreference(
			@RequestBody NotificationPreferenceRequest request) {
		preferenceService.updatePreference(
				request.getAccountId(),
				request.getNotificationType(),
				request.getChannel(),
				request.getEnabled(),
				request.getDestination()
				);
		return ResponseEntity.ok().build();
	}
	
}