package se.sundsvall.postportalservice.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.objectstore.ObjectStoreIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentUploadServiceTest {

	private static final String OBJECT_ID = "f8e2bd3c-1a6b-4f5e-9d0a-2c7b1e4f6a58";

	@Mock
	private ObjectStoreIntegration objectStoreIntegrationMock;

	@Test
	void storeOnce_uploadsTheFirstTime() {
		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenReturn(OBJECT_ID);
		final var attachment = attachment();

		final var objectId = service().storeOnce(attachment);

		assertThat(objectId).isEqualTo(OBJECT_ID);
		assertThat(attachment.getObjectId()).isEqualTo(OBJECT_ID);
		verify(objectStoreIntegrationMock).store(attachment);
	}

	@Test
	void storeOnce_secondCallReusesTheIdInsteadOfUploadingAgain() {
		// The whole point: delivery runs once per recipient against one shared attachment, so the second recipient
		// must get the first one's id rather than a second object holding the same bytes.
		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenReturn(OBJECT_ID);
		final var service = service();
		final var attachment = attachment();

		final var first = service.storeOnce(attachment);
		final var second = service.storeOnce(attachment);

		assertThat(second).isEqualTo(first);
		verify(objectStoreIntegrationMock, times(1)).store(attachment);
	}

	@Test
	void storeOnce_failureLeavesNothingRememberedSoTheNextRecipientRetries() {
		// A store outage must not poison the message: the id stays unset, and the recipient that follows tries again.
		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class)))
			.thenThrow(new IllegalStateException("store unavailable"))
			.thenReturn(OBJECT_ID);
		final var service = service();
		final var attachment = attachment();

		assertThat(attachment.getObjectId()).isNull();
		try {
			service.storeOnce(attachment);
		} catch (final IllegalStateException expected) {
			// The caller's catch is what marks this one recipient FAILED.
		}
		assertThat(attachment.getObjectId()).isNull();

		assertThat(service.storeOnce(attachment)).isEqualTo(OBJECT_ID);
		verify(objectStoreIntegrationMock, times(2)).store(attachment);
	}

	@Test
	void storeOnce_uploadsOncePerAttachmentNotOncePerRecipient() {
		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenAnswer(invocation -> UUID.randomUUID().toString());
		final var service = service();
		final var messageEntity = messageEntity(attachment(), attachment(), attachment());

		final var first = service.storeOnce(messageEntity);
		final var second = service.storeOnce(messageEntity);
		final var third = service.storeOnce(messageEntity);

		// Three attachments, three recipients: three uploads, not nine.
		verify(objectStoreIntegrationMock, times(3)).store(any(AttachmentEntity.class));
		assertThat(first).hasSize(3).doesNotHaveDuplicates();
		assertThat(second).isEqualTo(first);
		assertThat(third).isEqualTo(first);
	}

	@Test
	void storeOnce_handlesAMessageWithoutAttachments() {
		assertThat(service().storeOnce(messageEntity())).isEmpty();
		verify(objectStoreIntegrationMock, never()).store(any());
	}

	@Test
	void storeOnce_uploadsOnceEvenWhenRecipientThreadsRaceForIt() throws Exception {
		// This is the assertion a bare null check fails. Eight delivery threads share one attachment, so without the
		// lock several of them read null at the same time and each upload their own copy.
		final var uploads = new AtomicInteger();
		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenAnswer(invocation -> {
			uploads.incrementAndGet();
			// Widen the window a real network call would leave open anyway.
			Thread.sleep(20);
			return OBJECT_ID;
		});

		final var threads = 8;
		final var service = service();
		final var attachment = attachment();
		final var startLine = new CountDownLatch(1);
		final var finished = new CountDownLatch(threads);

		try (final var executor = Executors.newFixedThreadPool(threads)) {
			for (var i = 0; i < threads; i++) {
				executor.execute(() -> {
					try {
						startLine.await();
						service.storeOnce(attachment);
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						finished.countDown();
					}
				});
			}
			startLine.countDown();
			assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(uploads).hasValue(1);
		verify(objectStoreIntegrationMock, times(1)).store(attachment);
	}

	private AttachmentUploadService service() {
		return new AttachmentUploadService(objectStoreIntegrationMock);
	}

	private static AttachmentEntity attachment() {
		return AttachmentEntity.create()
			.withFileName("file.txt")
			.withContentType("text/plain");
	}

	private static MessageEntity messageEntity(final AttachmentEntity... attachments) {
		return MessageEntity.create()
			.withId("1a2b3c")
			.withAttachments(List.of(attachments));
	}
}
