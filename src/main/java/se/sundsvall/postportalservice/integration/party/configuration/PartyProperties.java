package se.sundsvall.postportalservice.integration.party.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("integration.party")
public record PartyProperties(
	@DefaultValue("5") int connectTimeout,
	@DefaultValue("30") int readTimeout) {
}
