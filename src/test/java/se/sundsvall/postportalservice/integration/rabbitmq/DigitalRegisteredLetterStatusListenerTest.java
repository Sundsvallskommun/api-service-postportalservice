package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.model.DigitalRegisteredLetterStatusEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalRegisteredLetterStatusListenerTest {

	@Mock
	private RecipientRepository recipientRepositoryMock;

	@Captor
	private ArgumentCaptor<RecipientEntity> recipientCaptor;

	@InjectMocks
	private DigitalRegisteredLetterStatusListener listener;

	@Test
	void handleStatusEvent() {
		final var recipientId = "recipient-id";
		final var externalId = "external-id";
		final var status = "SENT";
		final var statusDetail = "Letter delivered successfully";
		final var event = new DigitalRegisteredLetterStatusEvent(recipientId, externalId, status, statusDetail);
		final var recipient = RecipientEntity.create().withId(recipientId).withStatus("PENDING");

		when(recipientRepositoryMock.findById(recipientId)).thenReturn(Optional.of(recipient));

		listener.handleStatusEvent(event);

		verify(recipientRepositoryMock).findById(recipientId);
		verify(recipientRepositoryMock).save(recipientCaptor.capture());
		verifyNoMoreInteractions(recipientRepositoryMock);

		final var saved = recipientCaptor.getValue();
		assertThat(saved.getStatus()).isEqualTo(status);
		assertThat(saved.getExternalId()).isEqualTo(externalId);
		assertThat(saved.getStatusDetail()).isEqualTo(statusDetail);
	}

	@Test
	void handleStatusEvent_withNullStatusDetail() {
		final var recipientId = "recipient-id";
		final var externalId = "external-id";
		final var status = "SENT";
		final var event = new DigitalRegisteredLetterStatusEvent(recipientId, externalId, status, null);
		final var recipient = RecipientEntity.create().withId(recipientId).withStatus("PENDING");

		when(recipientRepositoryMock.findById(recipientId)).thenReturn(Optional.of(recipient));

		listener.handleStatusEvent(event);

		verify(recipientRepositoryMock).findById(recipientId);
		verify(recipientRepositoryMock).save(recipientCaptor.capture());
		verifyNoMoreInteractions(recipientRepositoryMock);

		final var saved = recipientCaptor.getValue();
		assertThat(saved.getStatus()).isEqualTo(status);
		assertThat(saved.getExternalId()).isEqualTo(externalId);
		assertThat(saved.getStatusDetail()).isNull();
	}

	@Test
	void handleStatusEvent_recipientNotFound() {
		final var recipientId = "non-existent-id";
		final var event = new DigitalRegisteredLetterStatusEvent(recipientId, "external-id", "FAILED", "Error");

		when(recipientRepositoryMock.findById(recipientId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> listener.handleStatusEvent(event))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Recipient with id '%s' not found".formatted(recipientId));

		verify(recipientRepositoryMock).findById(recipientId);
		verifyNoMoreInteractions(recipientRepositoryMock);
	}
}
