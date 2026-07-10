package se.sundsvall.postportalservice.service;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.api.model.EventSignatory;
import se.sundsvall.postportalservice.api.model.SignedDocument;
import se.sundsvall.postportalservice.api.model.SigningEvent;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.SigningEntity;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;
import se.sundsvall.postportalservice.service.util.BlobUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SigningEventServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String MESSAGE_ID = "550e8400-e29b-41d4-a716-446655440000";

	@Mock
	private MessageRepository messageRepositoryMock;
	@Mock
	private RecipientRepository recipientRepositoryMock;
	@Mock
	private SigningRepository signingRepositoryMock;
	@Mock
	private BlobUtil blobUtilMock;

	@InjectMocks
	private SigningEventService service;

	@Test
	void handleSigningEvent_completedStoresSignedDocumentAsNewAttachment() {
		final var message = MessageEntity.create().withId(MESSAGE_ID);
		final var signing = SigningEntity.create().withId("s1").withStatus("PENDING").withMessage(message);
		final var blob = mock(Blob.class);
		final var event = SigningEvent.create().withStatus("SIGNED")
			.withSignedDocument(SignedDocument.create().withFileName("signed.pdf").withMimeType("application/pdf").withContent("c2lnbmVk"));

		when(signingRepositoryMock.findByMessageId(MESSAGE_ID)).thenReturn(Optional.of(signing));
		when(blobUtilMock.convertBase64ToBlob("c2lnbmVk")).thenReturn(blob);

		service.handleSigningEvent(MUNICIPALITY_ID, MESSAGE_ID, event);

		assertThat(signing.getStatus()).isEqualTo("SIGNED");
		assertThat(message.getAttachments()).hasSize(1);
		final var signedAttachment = message.getAttachments().getFirst();
		assertThat(signedAttachment.getFileName()).isEqualTo("signed.pdf");
		assertThat(signedAttachment.getContentType()).isEqualTo("application/pdf");
		assertThat(signedAttachment.getContent()).isSameAs(blob);
		assertThat(signing.getAttachment()).isSameAs(signedAttachment);
		verify(messageRepositoryMock).save(message);
		verify(signingRepositoryMock).save(signing);
		verifyNoInteractions(recipientRepositoryMock);
	}

	@Test
	void handleSigningEvent_signatoryApprovedMarksRecipientSigned() {
		final var recipient = RecipientEntity.create().withId("r1").withPartyId("p1").withStatus("PENDING");
		final var message = MessageEntity.create().withId(MESSAGE_ID).withRecipients(new ArrayList<>(List.of(recipient)));
		final var signing = SigningEntity.create().withId("s1").withStatus("PENDING").withMessage(message);
		final var event = SigningEvent.create().withStatus("PENDING")
			.withSignatory(EventSignatory.create().withPartyId("p1").withAction("APPROVED"));

		when(signingRepositoryMock.findByMessageId(MESSAGE_ID)).thenReturn(Optional.of(signing));

		service.handleSigningEvent(MUNICIPALITY_ID, MESSAGE_ID, event);

		assertThat(recipient.getStatus()).isEqualTo("SIGNED");
		verify(recipientRepositoryMock).save(recipient);
		verify(signingRepositoryMock).save(signing);
		verifyNoInteractions(blobUtilMock, messageRepositoryMock);
	}

	@Test
	void handleSigningEvent_signatoryDeclinedMarksRecipientDeclinedWithReason() {
		final var recipient = RecipientEntity.create().withId("r1").withPartyId("p1").withStatus("PENDING");
		final var message = MessageEntity.create().withId(MESSAGE_ID).withRecipients(new ArrayList<>(List.of(recipient)));
		final var signing = SigningEntity.create().withId("s1").withStatus("PENDING").withMessage(message);
		final var event = SigningEvent.create().withStatus("FAILED")
			.withSignatory(EventSignatory.create().withPartyId("p1").withAction("DECLINED").withReason("Not authorised"));

		when(signingRepositoryMock.findByMessageId(MESSAGE_ID)).thenReturn(Optional.of(signing));

		service.handleSigningEvent(MUNICIPALITY_ID, MESSAGE_ID, event);

		assertThat(recipient.getStatus()).isEqualTo("DECLINED");
		assertThat(recipient.getStatusDetail()).isEqualTo("Not authorised");
		assertThat(signing.getStatus()).isEqualTo("FAILED");
		verify(recipientRepositoryMock).save(recipient);
	}

	@Test
	void handleSigningEvent_terminalStatusNotRegressed() {
		final var message = MessageEntity.create().withId(MESSAGE_ID);
		final var signing = SigningEntity.create().withId("s1").withStatus("SIGNED").withMessage(message);
		final var event = SigningEvent.create().withStatus("PENDING");

		when(signingRepositoryMock.findByMessageId(MESSAGE_ID)).thenReturn(Optional.of(signing));

		service.handleSigningEvent(MUNICIPALITY_ID, MESSAGE_ID, event);

		assertThat(signing.getStatus()).isEqualTo("SIGNED");
		verify(signingRepositoryMock).save(signing);
		verifyNoInteractions(recipientRepositoryMock, blobUtilMock, messageRepositoryMock);
	}

	@Test
	void handleSigningEvent_noSigningCaseIgnored() {
		when(signingRepositoryMock.findByMessageId("unknown")).thenReturn(Optional.empty());

		service.handleSigningEvent(MUNICIPALITY_ID, "unknown", SigningEvent.create().withStatus("SIGNED"));

		verify(signingRepositoryMock).findByMessageId("unknown");
		verifyNoMoreInteractions(signingRepositoryMock);
		verifyNoInteractions(messageRepositoryMock, recipientRepositoryMock, blobUtilMock);
	}
}
