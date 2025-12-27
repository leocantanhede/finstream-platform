package pt.lunasoft.transaction.mapper;

import org.springframework.stereotype.Component;

import pt.lunasoft.models.DeviceInfo;
import pt.lunasoft.models.Location;
import pt.lunasoft.models.Transaction;
import pt.lunasoft.transaction.entity.TransactionEntity;

@Component
public class TransactionMapper {

	public TransactionEntity toEntity(Transaction transaction) {
		TransactionEntity entity = TransactionEntity.builder()
				.id(transaction.getId())
				.accountId(transaction.getAccountId())
				.amount(transaction.getAmount())
				.currency(transaction.getCurrency())
				.type(transaction.getType())
				.merchant(transaction.getMerchant())
				.merchantCategory(transaction.getMerchantCategory())
				.timestamp(transaction.getTimestamp())
				.status(transaction.getStatus())
				.description(transaction.getDescription())
				.createdAt(transaction.getCreatedAt())
				.updatedAt(transaction.getUpdatedAt())
				.build();

		if (transaction.getLocation() != null) {
			Location loc = transaction.getLocation();
			entity.setLocationLatitude(loc.getLatitude());
			entity.setLocationLongitude(loc.getLongitude());
			entity.setLocationCity(loc.getCity());
			entity.setLocationCountry(loc.getCountry());
			entity.setIpAddress(loc.getIpAddress());
		}

		if (transaction.getDeviceInfo() != null) {
			DeviceInfo device = transaction.getDeviceInfo();
			entity.setDeviceId(device.getDeviceId());
			entity.setDeviceType(device.getDeviceType());
			entity.setOperatingSystem(device.getOperatingSystem());
			entity.setBrowser(device.getBrowser());
			entity.setUserAgent(device.getUserAgent());
		}

		return entity;
	}

	public Transaction toModel(TransactionEntity entity) {
		Transaction transaction = Transaction.builder()
				.id(entity.getId())
				.accountId(entity.getAccountId())
				.amount(entity.getAmount())
				.currency(entity.getCurrency())
				.type(entity.getType())
				.merchant(entity.getMerchant())
				.merchantCategory(entity.getMerchantCategory())
				.timestamp(entity.getTimestamp())
				.status(entity.getStatus())
				.description(entity.getDescription())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();

		if (entity.getLocationLatitude() != null) {
			transaction.setLocation(Location.builder()
					.latitude(entity.getLocationLatitude())
					.longitude(entity.getLocationLongitude())
					.city(entity.getLocationCity())
					.country(entity.getLocationCountry())
					.ipAddress(entity.getIpAddress())
					.build());
		}

		if (entity.getDeviceId() != null) {
			transaction.setDeviceInfo(DeviceInfo.builder()
					.deviceId(entity.getDeviceId())
					.deviceType(entity.getDeviceType())
					.operatingSystem(entity.getOperatingSystem())
					.browser(entity.getBrowser())
					.userAgent(entity.getUserAgent())
					.build());
		}

		return transaction;
	}

}