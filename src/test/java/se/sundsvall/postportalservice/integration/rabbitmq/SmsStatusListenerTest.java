package se.sundsvall.postportalservice.integration.rabbitmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.service.SmsStatusService;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SmsStatusListenerTest {

	private static final String RECIPIENT_ID = "8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9";
	private static final String EXTERNAL_ID = "550e8400-e29b-41d4-a716-446655440000";

	private static final SmsStatusMessage STATUS_MESSAGE = new SmsStatusMessage(RECIPIENT_ID, "SENT", EXTERNAL_ID, null);

	@Mock
	private SmsStatusService smsStatusServiceMock;

	@InjectMocks
	private SmsStatusListener listener;

	@BeforeEach
	@AfterEach
	void clearRecipientId() {
		// RecipientId keeps a counter in a thread-local, and other tests in this JVM can leave it above zero, which
		// would make init() a no-op and leave a stale id in the MDC.
		while (RecipientId.get() != null) {
			RecipientId.reset();
		}
	}

	@Test
	void receive() {
		listener.receive(STATUS_MESSAGE, "sms.sent");

		verify(smsStatusServiceMock).handleSmsStatus(STATUS_MESSAGE);
		verifyNoMoreInteractions(smsStatusServiceMock);
	}

	@Test
	void receive_actsOnThePayloadWhenTheRoutingKeyDisagrees() {
		// A disagreement is a producer bug; parking the message would strand the recipient at PENDING instead.
		listener.receive(STATUS_MESSAGE, "sms.failed");

		verify(smsStatusServiceMock).handleSmsStatus(STATUS_MESSAGE);
		verifyNoMoreInteractions(smsStatusServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"SENT", "sent", " Sent "
	})
	void resolve_recognisesSentRegardlessOfCasing(final String status) {
		final var resolved = listener.resolve(new SmsStatusMessage(RECIPIENT_ID, status, EXTERNAL_ID, null), "sms.sent");

		assertThat(resolved.status()).isEqualTo("SENT");
		assertThat(resolved.statusDetail()).isNull();
	}

	@Test
	void resolve_storesFailedForAnUnrecognisedOutcome() {
		final var resolved = listener.resolve(new SmsStatusMessage(RECIPIENT_ID, "DELIVERED", EXTERNAL_ID, null), null);

		// Never parked: the recipient would otherwise sit at PENDING with no second outcome coming.
		assertThat(resolved.status()).isEqualTo("FAILED");
		assertThat(resolved.statusDetail()).isEqualTo("Unrecognised outcome 'DELIVERED' reported by messaging");
		assertThat(resolved.externalId()).isEqualTo(EXTERNAL_ID);
	}

	@Test
	void resolve_fallsBackToTheRoutingKeyWhenThePayloadCarriesNoOutcome() {
		final var resolved = listener.resolve(new SmsStatusMessage(RECIPIENT_ID, null, EXTERNAL_ID, null), "sms.sent");

		assertThat(resolved.status()).isEqualTo("SENT");
		assertThat(resolved.statusDetail()).startsWith("Unrecognised outcome");
	}

	@Test
	void resolve_failsClosedWhenNeitherSourceCarriesAnOutcome() {
		final var resolved = listener.resolve(new SmsStatusMessage(RECIPIENT_ID, "  ", EXTERNAL_ID, null), "sms-status.dlq");

		assertThat(resolved.status()).isEqualTo("FAILED");
	}

	@Test
	void resolve_keepsTheMessageUntouchedWhenItIsAlreadyNormalised() {
		final var resolved = listener.resolve(STATUS_MESSAGE, "sms.sent");

		assertThat(resolved).isSameAs(STATUS_MESSAGE);
	}

	@Test
	void receive_toleratesAnAbsentRoutingKey() {
		// Dead-lettering rewrites the routing key, so a replayed message need not carry a recognisable one.
		listener.receive(STATUS_MESSAGE, null);

		verify(smsStatusServiceMock).handleSmsStatus(STATUS_MESSAGE);
		verifyNoMoreInteractions(smsStatusServiceMock);
	}

	@Test
	void receive_clearsRecipientIdFromMdc() {
		listener.receive(STATUS_MESSAGE, "sms.sent");

		assertThat(RecipientId.get()).isNull();
	}

	@Test
	void receive_clearsRecipientIdFromMdcOnFailure() {
		doThrow(new RuntimeException("boom")).when(smsStatusServiceMock).handleSmsStatus(STATUS_MESSAGE);

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> listener.receive(STATUS_MESSAGE, "sms.sent"));

		assertThat(RecipientId.get()).isNull();
	}
}
