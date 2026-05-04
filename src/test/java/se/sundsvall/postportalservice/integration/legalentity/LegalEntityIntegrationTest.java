package se.sundsvall.postportalservice.integration.legalentity;

import generated.se.sundsvall.legalentity.LegalEntity2;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class LegalEntityIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private LegalEntityClient clientMock;

	@InjectMocks
	private LegalEntityIntegration integration;

	@AfterEach
	void verifyNoMoreInteractionsAfter() {
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void fetchLegalEntities_empty() {
		assertThat(integration.getLegalEntities(MUNICIPALITY_ID, List.of())).isEmpty();
	}

	@Test
	void fetchLegalEntities_null() {
		assertThat(integration.getLegalEntities(MUNICIPALITY_ID, null)).isEmpty();
	}

	@Test
	void fetchLegalEntities_happyPath() {
		final var partyId1 = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
		final var partyId2 = "5b2c0b07-6b7e-4f8e-9b4f-9d6f5e9f8c7a";
		final var entity1 = new LegalEntity2();
		final var entity2 = new LegalEntity2();

		when(clientMock.getLegalEntity(MUNICIPALITY_ID, partyId1)).thenReturn(entity1);
		when(clientMock.getLegalEntity(MUNICIPALITY_ID, partyId2)).thenReturn(entity2);

		final var result = integration.getLegalEntities(MUNICIPALITY_ID, List.of(partyId1, partyId2));

		assertThat(result).hasSize(2)
			.containsEntry(partyId1, entity1)
			.containsEntry(partyId2, entity2);
		verify(clientMock).getLegalEntity(MUNICIPALITY_ID, partyId1);
		verify(clientMock).getLegalEntity(MUNICIPALITY_ID, partyId2);
	}

	@Test
	void fetchLegalEntities_partialFailure() {
		final var partyId1 = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
		final var partyId2 = "5b2c0b07-6b7e-4f8e-9b4f-9d6f5e9f8c7a";
		final var entity1 = new LegalEntity2();

		when(clientMock.getLegalEntity(MUNICIPALITY_ID, partyId1)).thenReturn(entity1);
		when(clientMock.getLegalEntity(MUNICIPALITY_ID, partyId2)).thenThrow(Problem.valueOf(BAD_GATEWAY, "kaboom"));

		final var result = integration.getLegalEntities(MUNICIPALITY_ID, List.of(partyId1, partyId2));

		assertThat(result).hasSize(1)
			.containsEntry(partyId1, entity1)
			.doesNotContainKey(partyId2);
		verify(clientMock).getLegalEntity(MUNICIPALITY_ID, partyId1);
		verify(clientMock).getLegalEntity(MUNICIPALITY_ID, partyId2);
	}

}
