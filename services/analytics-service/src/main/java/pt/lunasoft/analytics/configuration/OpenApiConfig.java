package pt.lunasoft.analytics.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().info(new Info().title("FinStream Analytics Service API").version("1.0.0")
				.description("API for analysis of financial transactions")
				.contact(new Contact().name("FinStream Team").email("lunasoft@lunasoft.pt"))
				.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")));
	}

}