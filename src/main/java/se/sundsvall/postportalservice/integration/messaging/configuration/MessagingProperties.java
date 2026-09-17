package se.sundsvall.postportalservice.integration.messaging.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("integration.messaging")
public record MessagingProperties(
	@DefaultValue("5") int connectTimeout,
	@DefaultValue("30") int readTimeout,
	@Valid @NotNull CallbackEmailSender callbackEmailSender) {

	/**
	 * Sender used on the callback e-mail that is sent when a letter can not be delivered as snail mail.
	 *
	 * @param name      display name of the sender, shared by all municipalities
	 * @param addresses sender e-mail address per municipality id
	 */
	public record CallbackEmailSender(
		@NotBlank String name,
		@NotEmpty Map<String, @NotBlank @Email String> addresses) {
	}
}
