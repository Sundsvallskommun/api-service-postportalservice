package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import generated.se.sundsvall.digitalregisteredletter.Letter;
import generated.se.sundsvall.digitalregisteredletter.LetterRequest;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;

@ExtendWith(MockitoExtension.class)
class DigitalRegisteredLetterIntegrationTest {

	private static final String HEADER_VALUE = "John Wick; type=adAccount";

	@Mock
	private DigitalRegisteredLetterClient clientMock;

	@Mock
	private DigitalRegisteredLetterMapper digitalRegisteredLetterMapperMock;

	@InjectMocks
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;

	@AfterEach
	void noMoreInteractions() {
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void checkKivraEligibility_AllEligible() {
		final var partyId1 = "123e4567-e89b-12d3-a456-426614174001";
		final var partyId2 = "123e4567-e89b-12d3-a456-426614174002";
		final var partyId3 = "123e4567-e89b-12d3-a456-426614174003";
		final var partyIds = List.of(partyId1, partyId2, partyId3);
		final var eligibilityRequest = digitalRegisteredLetterMapperMock.toEligibilityRequest(partyIds);
		when(clientMock.checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest)).thenReturn(partyIds);

		var result = digitalRegisteredLetterIntegration.checkKivraEligibility(MUNICIPALITY_ID, partyIds);

		assertThat(result).hasSameElementsAs(partyIds);
		verify(clientMock).checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest);
	}

	@Test
	void checkKivraEligibility_NoneEligible() {
		final var partyId1 = "123e4567-e89b-12d3-a456-426614174001";
		final var partyId2 = "123e4567-e89b-12d3-a456-426614174002";
		final var partyId3 = "123e4567-e89b-12d3-a456-426614174003";
		final var partyIds = List.of(partyId1, partyId2, partyId3);
		final var eligibilityRequest = digitalRegisteredLetterMapperMock.toEligibilityRequest(partyIds);

		final List<String> eligiblePartyIds = Collections.emptyList();
		when(clientMock.checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest)).thenReturn(eligiblePartyIds);

		var result = digitalRegisteredLetterIntegration.checkKivraEligibility(MUNICIPALITY_ID, partyIds);

		assertThat(result).isEmpty();
		verify(clientMock).checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest);
	}

	@Test
	void checkKivraEligibility_SomeEligible() {
		final var partyId1 = "123e4567-e89b-12d3-a456-426614174001";
		final var partyId2 = "123e4567-e89b-12d3-a456-426614174002";
		final var partyId3 = "123e4567-e89b-12d3-a456-426614174003";
		final var partyIds = List.of(partyId1, partyId2, partyId3);
		final var eligibilityRequest = digitalRegisteredLetterMapperMock.toEligibilityRequest(partyIds);

		final var eligiblePartyIds = List.of(partyId1, partyId2);
		when(clientMock.checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest)).thenReturn(eligiblePartyIds);

		var result = digitalRegisteredLetterIntegration.checkKivraEligibility(MUNICIPALITY_ID, partyIds);

		assertThat(result).hasSameElementsAs(eligiblePartyIds);
		verify(clientMock).checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest);
	}

	@Test
	void getAllLetters() {
		// TODO: Implement when logic is in place.
		var result = digitalRegisteredLetterIntegration.getAllLetters(MUNICIPALITY_ID);
		assertThat(result).isTrue();
	}

	@Test
	void getLetterById() {
		// TODO: Implement when logic is in place.
		var result = digitalRegisteredLetterIntegration.getLetterById(MUNICIPALITY_ID, "letterId");
		assertThat(result).isTrue();
	}

	@Test
	void sendLetter_happyCase() {
		final var userEntity = new UserEntity().withName("John Wick");
		final var messageEntity = new MessageEntity()
			.withUser(userEntity)
			.withMunicipalityId("2281");
		final var recipientEntity = new RecipientEntity();

		final var letterRequest = new LetterRequest();
		final var letter = new Letter()
			.status("SENT")
			.id("externalId");

		final var multipartMock = Mockito.mock(MultipartFile.class);
		final var multipartList = List.of(multipartMock);
		when(digitalRegisteredLetterMapperMock.toLetterRequest(messageEntity, recipientEntity)).thenReturn(letterRequest);
		when(digitalRegisteredLetterMapperMock.toMultipartFiles(any())).thenReturn(multipartList);
		when(clientMock.sendLetter(HEADER_VALUE, MUNICIPALITY_ID, letterRequest, multipartList)).thenReturn(letter);

		digitalRegisteredLetterIntegration.sendLetter(messageEntity, recipientEntity);

		assertThat(recipientEntity.getStatus()).isEqualTo("SENT");
		assertThat(recipientEntity.getExternalId()).isEqualTo("externalId");

		verify(digitalRegisteredLetterMapperMock).toLetterRequest(messageEntity, recipientEntity);
		verify(digitalRegisteredLetterMapperMock).toMultipartFiles(any());
		verify(clientMock).sendLetter(HEADER_VALUE, MUNICIPALITY_ID, letterRequest, multipartList);
	}

	@Test
	void sendLetter_clientThrowsException() {
		final var userEntity = new UserEntity().withName("John Wick");
		final var messageEntity = new MessageEntity()
			.withUser(userEntity)
			.withMunicipalityId("2281");
		final var recipientEntity = new RecipientEntity();

		final var letterRequest = new LetterRequest();

		final var multipartMock = Mockito.mock(MultipartFile.class);
		final var multipartList = List.of(multipartMock);
		when(digitalRegisteredLetterMapperMock.toLetterRequest(messageEntity, recipientEntity)).thenReturn(letterRequest);
		when(digitalRegisteredLetterMapperMock.toMultipartFiles(any())).thenReturn(multipartList);
		when(clientMock.sendLetter(HEADER_VALUE, MUNICIPALITY_ID, letterRequest, multipartList)).thenThrow(new RuntimeException("Some error"));

		digitalRegisteredLetterIntegration.sendLetter(messageEntity, recipientEntity);

		assertThat(recipientEntity.getStatus()).isEqualTo("FAILED");
		assertThat(recipientEntity.getStatusDetail()).isEqualTo("Some error");

		verify(digitalRegisteredLetterMapperMock).toLetterRequest(messageEntity, recipientEntity);
		verify(digitalRegisteredLetterMapperMock).toMultipartFiles(any());
		verify(clientMock).sendLetter(HEADER_VALUE, MUNICIPALITY_ID, letterRequest, multipartList);
	}

}
