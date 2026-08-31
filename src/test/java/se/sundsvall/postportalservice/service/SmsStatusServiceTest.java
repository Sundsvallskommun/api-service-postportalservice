package se.sundsvall.postportalservice.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsStatusMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.Constants.SENT;

@ExtendWith(MockitoExtension.class)
class SmsStatusServiceTest {

	private static final String RECIPIENT_ID = "8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9";
	private static final String EXTERNAL_ID = "550e8400-e29b-41d4-a716-446655440000";

	@Mock
	private RecipientRepository recipientRepositoryMock;

	@InjectMocks
	private SmsStatusService smsStatusService;

	@Test
	void handleSmsStatus_sent() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(PENDING);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, EXTERNAL_ID, null));

		assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
		assertThat(recipientEntity.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(recipientEntity.getStatusDetail()).isNull();
		verify(recipientRepositoryMock).findById(RECIPIENT_ID);
		verify(recipientRepositoryMock).save(recipientEntity);
		verifyNoMoreInteractions(recipientRepositoryMock);
	}

	@Test
	void handleSmsStatus_failed() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(PENDING);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, FAILED, EXTERNAL_ID, "Invalid mobile number"));

		assertThat(recipientEntity.getStatus()).isEqualTo(FAILED);
		assertThat(recipientEntity.getStatusDetail()).isEqualTo("Invalid mobile number");
		verify(recipientRepositoryMock).save(recipientEntity);
	}

	@Test
	void handleSmsStatus_blankStatusCountsAsFailure() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(PENDING);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, " ", EXTERNAL_ID, null));

		assertThat(recipientEntity.getStatus()).isEqualTo(FAILED);
		verify(recipientRepositoryMock).save(recipientEntity);
	}

	@Test
	void handleSmsStatus_keepsExistingExternalIdWhenNoneReported() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withExternalId(EXTERNAL_ID);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, null, null));

		assertThat(recipientEntity.getExternalId()).isEqualTo(EXTERNAL_ID);
	}

	@Test
	void handleSmsStatus_duplicateOutcomeIsIgnored() {
		// messaging publishes the outcome, confirms, then acks - a crash in between redelivers and duplicates it.
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(SENT).withExternalId(EXTERNAL_ID);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, "a-different-id", null));

		assertThat(recipientEntity.getExternalId()).isEqualTo(EXTERNAL_ID);
		verify(recipientRepositoryMock).findById(RECIPIENT_ID);
		verify(recipientRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(recipientRepositoryMock);
	}

	@Test
	void handleSmsStatus_sentIsNeverRevisedToFailed() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(SENT);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, FAILED, null, "Invalid mobile number"));

		assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
		assertThat(recipientEntity.getStatusDetail()).isNull();
		verify(recipientRepositoryMock, never()).save(any());
	}

	@Test
	void handleSmsStatus_sentCorrectsALocallyFailedRecipient() {
		// FAILED can have been written here on an unconfirmed publish that messaging nonetheless received.
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(FAILED).withStatusDetail("No broker confirmation");
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, EXTERNAL_ID, null));

		assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
		assertThat(recipientEntity.getExternalId()).isEqualTo(EXTERNAL_ID);
		verify(recipientRepositoryMock).save(recipientEntity);
	}

	@Test
	void handleSmsStatus_overlongExternalIdIsDroppedButTheOutcomeIsKept() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(PENDING);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		final var overlong = "x".repeat(37);
		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, overlong, null));

		// Losing the whole outcome to a failed insert would be worse than losing the id it arrived with.
		assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
		assertThat(recipientEntity.getExternalId()).isNull();
		verify(recipientRepositoryMock).save(recipientEntity);
	}

	@Test
	void handleSmsStatus_externalIdAtTheColumnWidthIsKept() {
		final var recipientEntity = RecipientEntity.create().withId(RECIPIENT_ID).withStatus(PENDING);
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipientEntity));

		final var exact = "x".repeat(36);
		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, exact, null));

		assertThat(recipientEntity.getExternalId()).isEqualTo(exact);
	}

	@Test
	void handleSmsStatus_unknownRecipientIsIgnored() {
		when(recipientRepositoryMock.findById(RECIPIENT_ID)).thenReturn(Optional.empty());

		smsStatusService.handleSmsStatus(new SmsStatusMessage(RECIPIENT_ID, SENT, EXTERNAL_ID, null));

		verify(recipientRepositoryMock).findById(RECIPIENT_ID);
		verify(recipientRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(recipientRepositoryMock);
	}
}
