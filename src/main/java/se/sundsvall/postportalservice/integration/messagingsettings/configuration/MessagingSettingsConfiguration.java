package se.sundsvall.postportalservice.integration.messagingsettings.configuration;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Configuration
@EnableConfigurationProperties(MessagingSettingsProperties.class)
@Import(FeignConfiguration.class)
public class MessagingSettingsConfiguration {

	public static final String CLIENT_ID = "messagingsettings";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final MessagingSettingsProperties messagingSettingsProperties, final ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value())))
			.withRequestTimeoutsInSeconds(messagingSettingsProperties.connectTimeout(), messagingSettingsProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
