package pt.lunasoft.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
	
	private String deviceId;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    private String userAgent;
    
}