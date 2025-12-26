package pt.lunasoft.transaction.entity;

import pt.lunasoft.models.enums.TransactionStatus;
import pt.lunasoft.models.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "TRANSACTION", indexes = {
		@Index(name = "IDX_ACCOUNT_ID", columnList = "ACCOUNT_ID"),
		@Index(name = "IDX_TIMESTAMP", columnList = "TIMESTAMP"),
		@Index(name = "IDX_STATUS", columnList = "STATUS")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ID")
	private UUID id;

	@Column(name = "ACCOUNT_ID", nullable = false, length = 50)
	private String accountId;

	@Column(name = "AMOUNT", nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(name = "CURRENCY", nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "TYPE", nullable = false, length = 20)
	private TransactionType type;

	@Column(name = "MERCHANT", nullable = false, length = 100)
	private String merchant;

	@Column(name = "MERCHANT_CATEGORY", length = 50)
	private String merchantCategory;

	@Column(name = "TIMESTAMP", nullable = false)
	private Instant timestamp;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false, length = 20)
	private TransactionStatus status;

	@Column(name = "DESCRIPTION", length = 500)
	private String description;

	@Column(name = "LOCATION_LATITUDE")
	private Double locationLatitude;

	@Column(name = "LOCATION_LONGITUDE")
	private Double locationLongitude;

	@Column(name = "LOCATION_CITY", length = 100)
	private String locationCity;

	@Column(name = "LOCATION_COUNTRY", length = 2)
	private String locationCountry;

	@Column(name = "IP_ADDRESS", length = 45)
	private String ipAddress;

	@Column(name = "DEVICE_ID", length = 100)
	private String deviceId;

	@Column(name = "DEVICE_TYPE", length = 50)
	private String deviceType;

	@Column(name = "OPERATING_SYSTEM", length = 50)
	private String operatingSystem;

	@Column(name = "BROWSER", length = 50)
	private String browser;

	@Column(name = "USER_AGENT", length = 500)
	private String userAgent;

	@CreationTimestamp
	@Column(name = "CREATED_AT", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "UPDATED_AT")
	private Instant updatedAt;

}