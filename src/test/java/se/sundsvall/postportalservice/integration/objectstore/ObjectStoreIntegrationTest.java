package se.sundsvall.postportalservice.integration.objectstore;

import java.sql.Blob;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.objectstore.configuration.ObjectStoreProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectStoreIntegrationTest {

	private static final String BUCKET = "messaging-attachments";
	private static final byte[] CONTENT = "hello world".getBytes();

	@Mock
	private ObjectStoreClient clientMock;

	@Mock
	private Blob blobMock;

	private ObjectStoreIntegration integration() {
		return integration("P7D");
	}

	private ObjectStoreIntegration integration(final String timeToLive) {
		return new ObjectStoreIntegration(clientMock, new ObjectStoreProperties(5, 30, BUCKET, timeToLive));
	}

	private void stubBlob() throws SQLException {
		when(blobMock.length()).thenReturn((long) CONTENT.length);
		when(blobMock.getBytes(1, CONTENT.length)).thenReturn(CONTENT);
	}

	private static AttachmentEntity attachment(final Blob content, final String contentType) {
		return AttachmentEntity.create()
			.withFileName("file.txt")
			.withContentType(contentType)
			.withContent(content);
	}

	@Test
	void store_returnsAFreshObjectId() throws SQLException {
		stubBlob();

		final var objectId = integration().store(attachment(blobMock, "text/plain"));

		// The id is minted here and is the only thing that travels on the queue.
		assertThat(objectId).isNotBlank();
		verify(clientMock).storeObject(eq(BUCKET), eq(objectId), anyString(), eq("text/plain"),
			eq("attachment; filename=\"file.txt\""), eq(CONTENT));
	}

	@Test
	void store_mintsADistinctIdPerCall() throws SQLException {
		stubBlob();

		final var first = integration().store(attachment(blobMock, "text/plain"));
		final var second = integration().store(attachment(blobMock, "text/plain"));

		// Two uploads of the same file are two objects: the store replaces on a repeated id, so sharing one would have
		// the second attachment silently overwrite the first.
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void store_fallsBackToOctetStreamWhenTheAttachmentHasNoContentType() throws SQLException {
		stubBlob();

		integration().store(attachment(blobMock, null));

		verify(clientMock).storeObject(anyString(), anyString(), anyString(), eq("application/octet-stream"), anyString(), any());
	}

	@Test
	void store_sendsAnExpiryDerivedFromTheConfiguredTimeToLive() throws SQLException {
		stubBlob();

		integration("PT1H").store(attachment(blobMock, "text/plain"));

		final var captor = ArgumentCaptor.forClass(String.class);
		verify(clientMock).storeObject(anyString(), anyString(), captor.capture(), anyString(), anyString(), any());

		// The object has to outlive the longest journey its reference can take - the retry ladder, the parking lot and
		// any replay from it - so the expiry is configuration rather than a constant.
		final var expiresAt = OffsetDateTime.parse(captor.getValue());
		assertThat(expiresAt).isAfter(OffsetDateTime.now().plusMinutes(50))
			.isBefore(OffsetDateTime.now().plusMinutes(70));
	}

	@Test
	void store_missingContentIsRefusedBeforeAnythingIsUploaded() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> integration().store(attachment(null, "text/plain")))
			.withMessageContaining("file.txt");

		verifyNoInteractions(clientMock);
	}

	@Test
	void store_unreadableContentIsRefusedBeforeAnythingIsUploaded() throws SQLException {
		when(blobMock.length()).thenThrow(new SQLException("connection lost"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> integration().store(attachment(blobMock, "text/plain")))
			.withMessageContaining("connection lost");

		// Failing here rather than uploading an empty object is what lets the caller mark the recipient FAILED without
		// having left a half-finished object behind.
		verifyNoInteractions(clientMock);
	}
}
