package se.sundsvall.postportalservice.integration.party;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
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
		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, null);

		assertThat(result).isEmpty();
	}

	@Test
	void getLegalIds() {
		final var expectedResult = Map.of(
			"28fba79e-73aa-4ecb-939f-301f326d2d4c", "191111111111",
			"f560865a-51f0-4e96-bca1-55d57a0d3f68", "192222222222");

		when(partyClientMock.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS)).thenReturn(expectedResult);

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, PARTY_IDS);

		assertThat(result).isEqualTo(expectedResult);
		verify(partyClientMock).getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS);
	}

	@Test
	void getLegalIds_empty() {
		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, List.of());

		assertThat(result).isEmpty();
	}

	@Test
	void getLegalIds_null() {
		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, null);

		assertThat(result).isEmpty();
	}

	@Test
	void getLegalIds_clientThrows() {
		when(partyClientMock.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Service unavailable"));

		assertThatThrownBy(() -> partyIntegration.getLegalIds(MUNICIPALITY_ID, PARTY_IDS))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway");

		verify(partyClientMock).getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS);
	}

	@Test
	void getLegalIdsInChunksWhenPartyIdsExceedMaxPerCall() {
		final var numberOfPartyIds = 2345;
		final var partyIds = IntStream.range(0, numberOfPartyIds)
			.mapToObj(i -> "partyId-" + i)
			.toList();

		when(partyClientMock.getPersonNumbers(anyString(), anyList()))
			.thenAnswer(invocation -> {
				final var ids = invocation.<List<String>>getArgument(1);
				return ids.stream()
					.collect(Collectors.toMap(Function.identity(), id -> "legalId-" + id));
			});

		final var result = partyIntegration.getLegalIds(MUNICIPALITY_ID, partyIds);

		assertThat(result).hasSize(numberOfPartyIds);
		verify(partyClientMock, times(3)).getPersonNumbers(anyString(), anyList());
		verifyNoMoreInteractions(partyClientMock);
	}

	@Test
	void getPartyIdsInChunksWhenLegalIdsExceedMaxPerCall() {
		final var numberOfPartyIds = 2345;
		final var partyIds = IntStream.range(0, numberOfPartyIds)
			.mapToObj(i -> "legalId-" + i)
			.toList();

		when(partyClientMock.getPartyIds(anyString(), anyList()))
			.thenAnswer(invocation -> {
				final var ids = invocation.<List<String>>getArgument(1);
				return ids.stream()
					.collect(Collectors.toMap(Function.identity(), id -> "partyId-" + id));
			});

		final var result = partyIntegration.getPartyIds(MUNICIPALITY_ID, partyIds);

		assertThat(result).hasSize(numberOfPartyIds);
		verify(partyClientMock, times(3)).getPartyIds(anyString(), anyList());
		verifyNoMoreInteractions(partyClientMock);
	}
}
