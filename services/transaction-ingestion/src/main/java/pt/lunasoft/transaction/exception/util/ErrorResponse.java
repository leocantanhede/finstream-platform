package pt.lunasoft.transaction.exception.util;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

	private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
	
}