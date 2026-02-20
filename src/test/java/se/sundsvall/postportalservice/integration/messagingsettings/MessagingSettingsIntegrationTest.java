package se.sundsvall.postportalservice.integration.messagingsettings;

import generated.se.sundsvall.messagingsettings.MessagingSettingValue;
import generated.se.sundsvall.messagingsettings.MessagingSettings;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.FOLDER_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SMS_SENDER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SUPPORT_TEXT;

@ExtendWith(MockitoExtension.class)
class MessagingSettingsIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String USERNAME = "testuser";
	private static final String HEADER_VALUE = "testuser; type=AD_ACCOUNT";

	@Mock
	private MessagingSettingsClient messagingSettingsClient;

	@InjectMocks
	private MessagingSettingsIntegration messagingSettingsIntegration;

	@BeforeEach
	void setup() {
		Identifier.set(Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue(USERNAME)
			.withTypeString("AD_ACCOUNT"));
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
		verifyNoMoreInteractions(messagingSettingsClient);
	}

	@Test
	void getMessagingSettingsForUser_success() {
		final var messagingSettings = createValidMessagingSettings();
		when(messagingSettingsClient.getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID))
			.thenReturn(List.of(messagingSettings));

		final var result = messagingSettingsIntegration.getMessagingSettingsForUser(MUNICIPALITY_ID);

		assertThat(result)
			.hasSize(9)
			.containsEntry(ORGANIZATION_NUMBER, "123456789")
			.containsEntry(FOLDER_NAME, "TestFolder")
			.containsEntry(SMS_SENDER, "Sundsvall")
			.containsEntry(SUPPORT_TEXT, "Support text")
			.containsEntry(CONTACT_INFORMATION_URL, "https://example.com")
			.containsEntry(CONTACT_INFORMATION_PHONE_NUMBER, "0123456789")
			.containsEntry(CONTACT_INFORMATION_EMAIL, "test@example.com")
			.containsEntry(DEPARTMENT_NAME, "Department 44")
			.containsEntry(DEPARTMENT_ID, "dept44");

		verify(messagingSettingsClient).getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID);
	}

	@Test
	void getMessagingSettingsForUser_emptySettings() {
		when(messagingSettingsClient.getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID))
			.thenReturn(List.of());

		assertThatThrownBy(() -> messagingSettingsIntegration.getMessagingSettingsForUser(MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: No messaging settings found for user '%s' in municipalityId '%s'"
				.formatted(USERNAME, MUNICIPALITY_ID));

		verify(messagingSettingsClient).getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID);
	}

	@Test
	void getMessagingSettingsForUser_multipleSettings() {
		final var messagingSettings1 = createValidMessagingSettings();
		final var messagingSettings2 = createValidMessagingSettings();
		when(messagingSettingsClient.getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID))
			.thenReturn(List.of(messagingSettings1, messagingSettings2));

		assertThatThrownBy(() -> messagingSettingsIntegration.getMessagingSettingsForUser(MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Found multiple messaging settings for user '%s' in municipalityId '%s', can't determine which one to use"
				.formatted(USERNAME, MUNICIPALITY_ID));

		verify(messagingSettingsClient).getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID);
	}

	@Test
	void getMessagingSettingsForUser_missingRequiredKey() {
		final var messagingSettings = new MessagingSettings().values(List.of(
			new MessagingSettingValue().key(ORGANIZATION_NUMBER).value("123456789"),
			new MessagingSettingValue().key(FOLDER_NAME).value("TestFolder"),
			new MessagingSettingValue().key(SMS_SENDER).value("Sundsvall"),
			new MessagingSettingValue().key(SUPPORT_TEXT).value("Support text"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_URL).value("https://example.com"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_PHONE_NUMBER).value("0123456789"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_EMAIL).value("test@example.com"),
			// DEPARTMENT_NAME is missing
			new MessagingSettingValue().key(DEPARTMENT_ID).value("dept44")));

		when(messagingSettingsClient.getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID))
			.thenReturn(List.of(messagingSettings));

		// Act & Assert
		assertThatThrownBy(() -> messagingSettingsIntegration.getMessagingSettingsForUser(MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Required messaging setting attribute '%s' is missing for user '%s' in municipalityId '%s'"
				.formatted(DEPARTMENT_NAME, USERNAME, MUNICIPALITY_ID));

		verify(messagingSettingsClient).getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID);
	}

	@Test
	void getMessagingSettingsForUser_invalidOrganizationNumber() {
		// Arrange - Create settings with invalid organization_number (contains non-digits)
		final var messagingSettings = new MessagingSettings().values(List.of(
			new MessagingSettingValue().key(ORGANIZATION_NUMBER).value("ABC-123"),
			new MessagingSettingValue().key(FOLDER_NAME).value("TestFolder"),
			new MessagingSettingValue().key(SMS_SENDER).value("Sundsvall"),
			new MessagingSettingValue().key(SUPPORT_TEXT).value("Support text"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_URL).value("https://example.com"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_PHONE_NUMBER).value("0123456789"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_EMAIL).value("test@example.com"),
			new MessagingSettingValue().key(DEPARTMENT_NAME).value("Department 44"),
			new MessagingSettingValue().key(DEPARTMENT_ID).value("dept44")));

		when(messagingSettingsClient.getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID))
			.thenReturn(List.of(messagingSettings));

		assertThatThrownBy(() -> messagingSettingsIntegration.getMessagingSettingsForUser(MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Invalid format for messaging setting attribute '%s' for user '%s' in municipalityId '%s'"
				.formatted(ORGANIZATION_NUMBER, USERNAME, MUNICIPALITY_ID));

		verify(messagingSettingsClient).getMessagingSettingsForUser(HEADER_VALUE, MUNICIPALITY_ID);
	}

	private MessagingSettings createValidMessagingSettings() {
		return new MessagingSettings().values(List.of(
			new MessagingSettingValue().key(ORGANIZATION_NUMBER).value("123456789"),
			new MessagingSettingValue().key(FOLDER_NAME).value("TestFolder"),
			new MessagingSettingValue().key(SMS_SENDER).value("Sundsvall"),
			new MessagingSettingValue().key(SUPPORT_TEXT).value("Support text"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_URL).value("https://example.com"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_PHONE_NUMBER).value("0123456789"),
			new MessagingSettingValue().key(CONTACT_INFORMATION_EMAIL).value("test@example.com"),
			new MessagingSettingValue().key(DEPARTMENT_NAME).value("Department 44"),
			new MessagingSettingValue().key(DEPARTMENT_ID).value("dept44")));
	}

}
