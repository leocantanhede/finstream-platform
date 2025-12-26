package pt.lunasoft.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

	private UUID id;
	
	@NotBlank(message = "Account ID is required")
	private String accountId;
	
	@NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
	private BigDecimal amount;
	
	@NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
	private String currency;
	
	@NotNull(message = "Transaction type is required")
	private TransactionType type;
	
	@NotBlank(message = "Merchant is required")
	private String merchant;
	private String merchantCategory;
	
	@NotNull(message = "Timestamp is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant timestamp;
	private TransactionStatus status;
	private String description;
	private Location location;
	private DeviceInfo deviceInfo;
	private Instant createAt;
	private Instant updateAt;
	
}