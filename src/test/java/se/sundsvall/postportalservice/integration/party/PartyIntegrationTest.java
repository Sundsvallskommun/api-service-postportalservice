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
	private static final Set<String> LEGAL_IDS_SET = Set.of("191111111111", "192222222222");
	private static final List<String> LEGAL_IDS_LIST = List.of("191111111111", "192222222222");
	private static final List<String> PARTY_IDS = List.of("28fba79e-73aa-4ecb-939f-301f326d2d4c", "f560865a-51f0-4e96-bca1-55d57a0d3f68");

	@Mock
	private PartyClient partyClientMock;

	@InjectMocks
	private PartyIntegration partyIntegration;

	@AfterEach
	void verifyInteractions() {
		verifyNoMoreInteractions(partyClientMock);
	}

	@Test
	void getPartyIds_withSet() {
		final var expectedResult = Map.of(
			"191111111111", "28fba79e-73aa-4ecb-939f-301f326d2d4c",
			"192222222222", "f560865a-51f0-4e96-bca1-55d57a0d3f68");

		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS_SET))).thenReturn(expectedResult);

		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, LEGAL_IDS_SET);

		assertThat(result).isEqualTo(expectedResult);
		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS_SET));
	}

	@Test
	void getPartyIds_withSet_empty() {
		final Map<String, String> expectedResult = Map.of();

		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, List.of())).thenReturn(expectedResult);

		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, Set.of());

		assertThat(result).isEmpty();
		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, List.of());
	}

	@Test
	void getPartyIds_withList() {
		final var expectedResult = Map.of(
			"191111111111", "28fba79e-73aa-4ecb-939f-301f326d2d4c",
			"192222222222", "f560865a-51f0-4e96-bca1-55d57a0d3f68");

		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, LEGAL_IDS_LIST)).thenReturn(expectedResult);

		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, LEGAL_IDS_LIST);

		assertThat(result).isEqualTo(expectedResult);
		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, LEGAL_IDS_LIST);
	}

	@Test
	void getPartyIds_withList_empty() {
		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, List.of());

		assertThat(result).isEmpty();
	}

	@Test
	void getPartyIds_withList_null() {
		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, (List<String>) null);

		assertThat(result).isEmpty();
	}

	@Test
	void getPartyIds_clientThrows() {
		when(partyClientMock.getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS_SET)))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Service unavailable"));

		assertThatThrownBy(() -> partyIntegration.getPartyIds(MUNICIPALITY_ID, LEGAL_IDS_SET))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway");

		verify(partyClientMock).getPartyIds(MUNICIPALITY_ID, List.copyOf(LEGAL_IDS_SET));
	}

	@Test
	void getPersonNumbers() {
		final var expectedResult = Map.of(
			"28fba79e-73aa-4ecb-939f-301f326d2d4c", "191111111111",
			"f560865a-51f0-4e96-bca1-55d57a0d3f68", "192222222222");

		when(partyClientMock.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS)).thenReturn(expectedResult);

		final var result = partyIntegration.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS);

		assertThat(result).isEqualTo(expectedResult);
		verify(partyClientMock).getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS);
	}

	@Test
	void getPersonNumbers_empty() {
		final var result = partyIntegration.getPersonNumbers(MUNICIPALITY_ID, List.of());

		assertThat(result).isEmpty();
	}

	@Test
	void getPersonNumbers_null() {
		final var result = partyIntegration.getPersonNumbers(MUNICIPALITY_ID, null);

		assertThat(result).isEmpty();
	}

	@Test
	void getPersonNumbers_clientThrows() {
		when(partyClientMock.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Service unavailable"));

		assertThatThrownBy(() -> partyIntegration.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway");

		verify(partyClientMock).getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS);
	}
}
