package se.sundsvall.postportalservice.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.RecipientResponse;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.RecipientMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String RECIPIENT_ID = "recipientId";

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Mock
	private RecipientMapper recipientMapperMock;

	@InjectMocks
	private RecipientService recipientService;

	@Test
	void getRecipient() {
		final var recipient = RecipientEntity.create().withId(RECIPIENT_ID).withPartyId("partyId");
		final var user = new UserEntity().withUsername("joe01doe");
		final var message = MessageEntity.create()
			.withUser(user)
			.withDepartment(DepartmentEntity.create().withName("dept"))
			.withRecipients(List.of(recipient));
		final var settingsMap = Map.of("key", "value");
		final var expectedResponse = new RecipientResponse("partyId", "subject", "body", "text/html",
			"123", "dept", "support", "url", "email", "phone", "joe01doe", List.of());

		when(messageRepositoryMock.findByMunicipalityIdAndRecipients_Id(MUNICIPALITY_ID, RECIPIENT_ID)).thenReturn(Optional.of(message));
		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID, "joe01doe")).thenReturn(settingsMap);
		when(recipientMapperMock.toRecipientResponse(message, recipient, settingsMap)).thenReturn(expectedResponse);

		final var result = recipientService.getRecipient(MUNICIPALITY_ID, RECIPIENT_ID);

		assertThat(result).isEqualTo(expectedResponse);
		verify(messageRepositoryMock).findByMunicipalityIdAndRecipients_Id(MUNICIPALITY_ID, RECIPIENT_ID);
		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID, "joe01doe");
		verify(recipientMapperMock).toRecipientResponse(message, recipient, settingsMap);
	}

	@Test
	void getRecipient_messageNotFound() {
		when(messageRepositoryMock.findByMunicipalityIdAndRecipients_Id(MUNICIPALITY_ID, RECIPIENT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recipientService.getRecipient(MUNICIPALITY_ID, RECIPIENT_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No message found for recipient with id '%s' in municipality '%s'".formatted(RECIPIENT_ID, MUNICIPALITY_ID));

		verifyNoInteractions(messagingSettingsIntegrationMock, recipientMapperMock);
	}

	@Test
	void getRecipient_recipientNotFoundInMessage() {
		final var otherRecipient = RecipientEntity.create().withId("otherId");
		final var user = new UserEntity().withUsername("joe01doe");
		final var message = MessageEntity.create()
			.withUser(user)
			.withDepartment(DepartmentEntity.create())
			.withRecipients(List.of(otherRecipient));

		when(messageRepositoryMock.findByMunicipalityIdAndRecipients_Id(MUNICIPALITY_ID, RECIPIENT_ID)).thenReturn(Optional.of(message));

		assertThatThrownBy(() -> recipientService.getRecipient(MUNICIPALITY_ID, RECIPIENT_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Recipient with id '%s' not found".formatted(RECIPIENT_ID));

		verifyNoInteractions(recipientMapperMock);
	}
}
