package se.sundsvall.postportalservice.integration.rabbitmq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StatusListenerTest {

	@Mock
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegrationMock;

	@InjectMocks
	private StatusListener statusListener;

	@Test
	void handleEvent() {
		statusListener.handleEvent("externalId");

		verifyNoInteractions(digitalRegisteredLetterIntegrationMock);
	}
}
