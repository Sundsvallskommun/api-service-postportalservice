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
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.SigningEntity;
import se.sundsvall.postportalservice.integration.db.dao.AttachmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;
import se.sundsvall.postportalservice.service.util.BlobUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SigningEventServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private SigningRepository signingRepositoryMock;
	@Mock
	private MessageRepository messageRepositoryMock;
	@Mock
	private RecipientRepository recipientRepositoryMock;
	@Mock
	private AttachmentRepository attachmentRepositoryMock;
	@Mock
	private BlobUtil blobUtilMock;

	@InjectMocks
	private SigningEventService service;

	@Test
	void handleSigningEvent_completedOverwritesDocument() {
		final var signing = SigningEntity.create().withId("s1").withMessageId("m1").withAttachmentId("a1").withStatus("INVANTAR_SIGNERING");
		final var attachment = AttachmentEntity.create().withId("a1");
		final var blob = mock(Blob.class);
		final var event = SigningEvent.create().withProviderCaseId("pc1").withStatus("SIGNERAT")
			.withSignedDocument(SignedDocument.create().withFileName("signed.pdf").withContent("c2lnbmVk"));

		when(signingRepositoryMock.findByProviderCaseId("pc1")).thenReturn(Optional.of(signing));
		when(attachmentRepositoryMock.findById("a1")).thenReturn(Optional.of(attachment));
		when(blobUtilMock.convertBase64ToBlob("c2lnbmVk")).thenReturn(blob);

		service.handleSigningEvent(MUNICIPALITY_ID, event);

		assertThat(signing.getStatus()).isEqualTo("SIGNERAT");
		assertThat(attachment.getContent()).isSameAs(blob);
		verify(attachmentRepositoryMock).save(attachment);
		verify(signingRepositoryMock).save(signing);
	}

	@Test
	void handleSigningEvent_signatoryApprovedMarksRecipientSigned() {
		final var recipient = RecipientEntity.create().withId("r1").withPartyId("p1").withStatus("PENDING");
		final var message = MessageEntity.create().withId("m1").withRecipients(new ArrayList<>(List.of(recipient)));
		final var signing = SigningEntity.create().withId("s1").withMessageId("m1").withStatus("INVANTAR_SIGNERING");
		final var event = SigningEvent.create().withProviderCaseId("pc1").withStatus("INVANTAR_SIGNERING")
			.withSignatory(EventSignatory.create().withPartyId("p1").withAction("APPROVED"));

		when(signingRepositoryMock.findByProviderCaseId("pc1")).thenReturn(Optional.of(signing));
		when(messageRepositoryMock.findById("m1")).thenReturn(Optional.of(message));

		service.handleSigningEvent(MUNICIPALITY_ID, event);

		assertThat(recipient.getStatus()).isEqualTo("SIGNED");
		verify(recipientRepositoryMock).save(recipient);
		verify(signingRepositoryMock).save(signing);
		verifyNoInteractions(attachmentRepositoryMock, blobUtilMock);
	}

	@Test
	void handleSigningEvent_signatoryDeclinedMarksRecipientDeclinedWithReason() {
		final var recipient = RecipientEntity.create().withId("r1").withPartyId("p1").withStatus("PENDING");
		final var message = MessageEntity.create().withId("m1").withRecipients(new ArrayList<>(List.of(recipient)));
		final var signing = SigningEntity.create().withId("s1").withMessageId("m1").withStatus("INVANTAR_SIGNERING");
		final var event = SigningEvent.create().withProviderCaseId("pc1").withStatus("FEL")
			.withSignatory(EventSignatory.create().withPartyId("p1").withAction("DECLINED").withReason("Not authorised"));

		when(signingRepositoryMock.findByProviderCaseId("pc1")).thenReturn(Optional.of(signing));
		when(messageRepositoryMock.findById("m1")).thenReturn(Optional.of(message));

		service.handleSigningEvent(MUNICIPALITY_ID, event);

		assertThat(recipient.getStatus()).isEqualTo("DECLINED");
		assertThat(recipient.getStatusDetail()).isEqualTo("Not authorised");
		assertThat(signing.getStatus()).isEqualTo("FEL");
		verify(recipientRepositoryMock).save(recipient);
	}

	@Test
	void handleSigningEvent_terminalStatusIsNotRegressed() {
		final var signing = SigningEntity.create().withId("s1").withStatus("SIGNERAT");
		final var event = SigningEvent.create().withProviderCaseId("pc1").withStatus("INVANTAR_SIGNERING");

		when(signingRepositoryMock.findByProviderCaseId("pc1")).thenReturn(Optional.of(signing));

		service.handleSigningEvent(MUNICIPALITY_ID, event);

		assertThat(signing.getStatus()).isEqualTo("SIGNERAT");
		verify(signingRepositoryMock).save(signing);
		verifyNoInteractions(messageRepositoryMock, recipientRepositoryMock, attachmentRepositoryMock, blobUtilMock);
	}

	@Test
	void handleSigningEvent_unknownCaseIsIgnored() {
		final var event = SigningEvent.create().withProviderCaseId("unknown").withStatus("SIGNERAT");

		when(signingRepositoryMock.findByProviderCaseId("unknown")).thenReturn(Optional.empty());

		service.handleSigningEvent(MUNICIPALITY_ID, event);

		verify(signingRepositoryMock).findByProviderCaseId("unknown");
		verify(signingRepositoryMock, never()).save(any());
		verifyNoInteractions(messageRepositoryMock, recipientRepositoryMock, attachmentRepositoryMock, blobUtilMock);
	}
}
