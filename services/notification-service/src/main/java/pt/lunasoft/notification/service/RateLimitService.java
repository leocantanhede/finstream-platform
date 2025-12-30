package pt.lunasoft.notification.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pt.lunasoft.notification.enums.NotificationChannel;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

	private final RedisTemplate<String, String> redisTemplate;

	@Value("${app.rate-limit.email.per-account}")
	private int emailLimitPerAccount;

	@Value("${app.rate-limit.email.window-minutes}")
	private int emailWindowMinutes;

	@Value("${app.rate-limit.sms.per-account}")
	private int smsLimitPerAccount;

	@Value("${app.rate-limit.sms.window-minutes}")
	private int smsWindowMinutes;

	public boolean allowNotification(String recipientId, NotificationChannel channel) {
		String key = buildRateLimitKey(recipientId, channel);

		int limit = getLimit(channel);
		int windowMinutes = getWindowMinutes(channel);

		String currentCount = redisTemplate.opsForValue().get(key);

		if (currentCount == null) {
			redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
			return true;
		}

		int count = Integer.parseInt(currentCount);

		if (count >= limit) {
			log.warn("Rate limit exceeded for {} on channel {}: {}/{}", 
					recipientId, channel, count, limit);
			return false;
		}

		redisTemplate.opsForValue().increment(key);
		return true;
	}

	private String buildRateLimitKey(String recipientId, NotificationChannel channel) {
		return String.format("rate:limit:%s:%s", channel.toString().toLowerCase(), recipientId);
	}

	private int getLimit(NotificationChannel channel) {
		return switch (channel) {
		case EMAIL -> emailLimitPerAccount;
		case SMS -> smsLimitPerAccount;
		default -> 100; // Default limit
		};
	}

	private int getWindowMinutes(NotificationChannel channel) {
		return switch (channel) {
		case EMAIL -> emailWindowMinutes;
		case SMS -> smsWindowMinutes;
		default -> 60;
		};
	}
	
}