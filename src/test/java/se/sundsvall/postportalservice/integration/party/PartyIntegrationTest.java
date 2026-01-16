package se.sundsvall.postportalservice.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final Set<String> LEGAL_IDS = Set.of("191111111111", "192222222222");

	@Mock
	private PartyClient partyClientMock;

	@InjectMocks
	private PartyIntegration partyIntegration;

	@AfterEach
	void verifyInteractions() {
		verifyNoMoreInteractions(partyClientMock);
	}

	@Test
	void getPartyIds() {
		final var expectedResult = Map.of(
			"191111111111", "28fba79e-73aa-4ecb-939f-301f326d2d4c",
			"192222222222", "f560865a-51f0-4e96-bca1-55d57a0d3f68");

		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS))).thenReturn(expectedResult);

		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, LEGAL_IDS);

		assertThat(result).isEqualTo(expectedResult);
		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS));
	}

	@Test
	void getPartyIds_withEmptySet() {
		final Map<String, String> expectedResult = Map.of();

		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, List.of())).thenReturn(expectedResult);

		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, Set.of());

		assertThat(result).isEmpty();
		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, List.of());
	}

	@Test
	void getPartyIds_clientThrows() {
		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS)))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Service unavailable"));

		assertThatThrownBy(() -> partyIntegration.getPartyIds(MUNICIPALITY_ID, LEGAL_IDS))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway");

		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS));
	}
}
