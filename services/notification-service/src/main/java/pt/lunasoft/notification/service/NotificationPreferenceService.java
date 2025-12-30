package pt.lunasoft.notification.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.entity.NotificationPreferenceEntity;
import pt.lunasoft.notification.enums.NotificationChannel;
import pt.lunasoft.notification.enums.NotificationType;
import pt.lunasoft.notification.repository.NotificationPreferenceRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

	private final NotificationPreferenceRepository preferenceRepository;

	@Cacheable(value = "notification-preferences", key = "#accountId + '-' + #type + '-' + #channel")
	public boolean isEnabled(String accountId, NotificationType type, NotificationChannel channel) {
		return preferenceRepository
				.findByAccountIdAndNotificationTypeAndChannel(accountId, type, channel)
				.map(NotificationPreferenceEntity::getEnabled)
				.orElse(true); // Default to enabled if no preference set
	}

	@Transactional
	public void updatePreference(
			String accountId,
			NotificationType type,
			NotificationChannel channel,
			boolean enabled,
			String destination) {

		NotificationPreferenceEntity preference = preferenceRepository
				.findByAccountIdAndNotificationTypeAndChannel(accountId, type, channel)
				.orElse(NotificationPreferenceEntity.builder()
						.accountId(accountId)
						.notificationType(type)
						.channel(channel)
						.build());

		preference.setEnabled(enabled);
		preference.setDestination(destination);

		preferenceRepository.save(preference);
		log.info("Updated notification preference for account: {}", accountId);
	}

	public List<NotificationPreferenceEntity> getPreferences(String accountId) {
		return preferenceRepository.findByAccountId(accountId);
	}
	
}