package pt.lunasoft.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableKafka
@EnableAsync
public class FraudDetectionApplication {

	public static void main(String [] args) {
		SpringApplication.run(FraudDetectionApplication.class, args);
	}

}
