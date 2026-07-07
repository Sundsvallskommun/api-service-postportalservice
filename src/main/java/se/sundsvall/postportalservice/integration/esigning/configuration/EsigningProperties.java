package se.sundsvall.postportalservice.integration.esigning.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("integration.esigning")
public record EsigningProperties(
	@DefaultValue("5") int connectTimeout,
	@DefaultValue("30") int readTimeout) {
}
