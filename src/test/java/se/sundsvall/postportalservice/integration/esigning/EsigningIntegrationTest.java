package se.sundsvall.postportalservice.integration.esigning;

import generated.se.sundsvall.esigning.StartSigningRequest;
import generated.se.sundsvall.esigning.StartSigningResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsigningIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private EsigningClient clientMock;

	@InjectMocks
	private EsigningIntegration esigningIntegration;

	@Test
	void createSigning() {
		final var request = new StartSigningRequest();
		final var response = new StartSigningResponse().providerCaseId("case-1");
		when(clientMock.createSigning(MUNICIPALITY_ID, request)).thenReturn(response);

		final var result = esigningIntegration.createSigning(MUNICIPALITY_ID, request);

		assertThat(result).isSameAs(response);
		verify(clientMock).createSigning(MUNICIPALITY_ID, request);
		verifyNoMoreInteractions(clientMock);
	}
}
