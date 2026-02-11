package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import feign.Response;
import generated.se.sundsvall.digitalregisteredletter.EligibilityRequest;
import generated.se.sundsvall.digitalregisteredletter.Letter;
import generated.se.sundsvall.digitalregisteredletter.LetterRequest;
import generated.se.sundsvall.digitalregisteredletter.LetterStatus;
import generated.se.sundsvall.digitalregisteredletter.LetterStatusRequest;
import generated.se.sundsvall.digitalregisteredletter.SigningInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;

@ExtendWith(MockitoExtension.class)
class DigitalRegisteredLetterIntegrationTest {

	private static final String HEADER_VALUE = "John Wick; type=adAccount";
	private static final String PARTY_ID_1 = "123e4567-e89b-12d3-a456-426614174001";
	private static final String PARTY_ID_2 = "123e4567-e89b-12d3-a456-426614174002";
	private static final String PARTY_ID_3 = "123e4567-e89b-12d3-a456-426614174003";
	private static final String LETTER_ID = "123e4567-e89b-12d3-a456-426614174001";

	@Mock
	private DigitalRegisteredLetterClient clientMock;

	@Mock
	private DigitalRegisteredLetterMapper digitalRegisteredLetterMapperMock;

	@InjectMocks
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;

	@AfterEach
	void noMoreInteractions() {
		verifyNoMoreInteractions(clientMock, digitalRegisteredLetterMapperMock);
	}

	@Test
	void checkKivraEligibility() {
		final var partyIds = List.of(PARTY_ID_1, PARTY_ID_2, PARTY_ID_3);
		final var eligibilityRequest = new EligibilityRequest();
		final var eligiblePartyIds = List.of(PARTY_ID_1, PARTY_ID_2);

		when(digitalRegisteredLetterMapperMock.toEligibilityRequest(partyIds)).thenReturn(eligibilityRequest);
		when(clientMock.checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest)).thenReturn(eligiblePartyIds);

		final var result = digitalRegisteredLetterIntegration.checkKivraEligibility(MUNICIPALITY_ID, partyIds);

		assertThat(result).isEqualTo(eligiblePartyIds);
		verify(digitalRegisteredLetterMapperMock).toEligibilityRequest(partyIds);
		verify(clientMock).checkKivraEligibility(MUNICIPALITY_ID, eligibilityRequest);
	}

	@Test
	void sendLetter_happyCase() {
		final var userEntity = new UserEntity().withUsername("John Wick");
		final var messageEntity = new MessageEntity()
			.withUser(userEntity)
			.withMunicipalityId(MUNICIPALITY_ID);
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
		final var userEntity = new UserEntity().withUsername("John Wick");
		final var messageEntity = new MessageEntity()
			.withUser(userEntity)
			.withMunicipalityId(MUNICIPALITY_ID);
		final var recipientEntity = new RecipientEntity();

		final var letterRequest = new LetterRequest();

		final var multipartMock = Mockito.mock(MultipartFile.class);
		final var multipartList = List.of(multipartMock);
		when(digitalRegisteredLetterMapperMock.toLetterRequest(messageEntity, recipientEntity)).thenReturn(letterRequest);
		when(digitalRegisteredLetterMapperMock.toMultipartFiles(any())).thenReturn(multipartList);
		when(clientMock.sendLetter(HEADER_VALUE, MUNICIPALITY_ID, letterRequest, multipartList)).thenThrow(new RuntimeException("Some error"));

		digitalRegisteredLetterIntegration.sendLetter(messageEntity, recipientEntity);

		assertThat(recipientEntity.getStatus()).isEqualTo("FAILED");
		assertThat(recipientEntity.getStatusDetail()).isEqualTo("RuntimeException: Some error");

		verify(digitalRegisteredLetterMapperMock).toLetterRequest(messageEntity, recipientEntity);
		verify(digitalRegisteredLetterMapperMock).toMultipartFiles(any());
		verify(clientMock).sendLetter(HEADER_VALUE, MUNICIPALITY_ID, letterRequest, multipartList);
	}

	@Test
	void getSigningInformation() {
		final var signingInfo = new SigningInfo();
		final var signingInformation = new SigningInformation();

		when(clientMock.getSigningInfo(MUNICIPALITY_ID, LETTER_ID)).thenReturn(signingInfo);
		when(digitalRegisteredLetterMapperMock.toSigningInformation(signingInfo)).thenReturn(signingInformation);

		final var response = digitalRegisteredLetterIntegration.getSigningInformation(MUNICIPALITY_ID, LETTER_ID);

		assertThat(response).isEqualTo(signingInformation);
		verify(clientMock).getSigningInfo(MUNICIPALITY_ID, LETTER_ID);
		verify(digitalRegisteredLetterMapperMock).toSigningInformation(signingInfo);
	}

	@Test
	void getLetterStatuses() {
		final var letterStatusRequest = new LetterStatusRequest();
		final var letterStatus = new LetterStatus();

		when(digitalRegisteredLetterMapperMock.toLetterStatusRequest(List.of(LETTER_ID))).thenReturn(letterStatusRequest);
		when(clientMock.getLetterStatuses(MUNICIPALITY_ID, letterStatusRequest)).thenReturn(List.of(letterStatus));

		final var response = digitalRegisteredLetterIntegration.getLetterStatuses(MUNICIPALITY_ID, List.of(LETTER_ID));

		assertThat(response).isEqualTo(List.of(letterStatus));
		verify(digitalRegisteredLetterMapperMock).toLetterStatusRequest(List.of(LETTER_ID));
		verify(clientMock).getLetterStatuses(MUNICIPALITY_ID, letterStatusRequest);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void getLetterStatuses_nullAndEmpty(final List<String> ids) {
		assertThat(digitalRegisteredLetterIntegration.getLetterStatuses(MUNICIPALITY_ID, ids)).isEmpty();
	}

	@Test
	void getLetterReceipt() throws Exception {
		final var pdfData = "test receipt data".getBytes();

		final Response mockFeignResponse = mock(Response.class);
		final Response.Body mockBody = mock(Response.Body.class);
		final Map<String, Collection<String>> headers = new HashMap<>();
		headers.put("Content-Type", List.of("application/pdf"));
		headers.put("Content-Disposition", List.of("attachment; filename=receipt.pdf"));

		when(mockFeignResponse.status()).thenReturn(200);
		when(mockFeignResponse.headers()).thenReturn(headers);
		when(mockFeignResponse.body()).thenReturn(mockBody);
		when(mockBody.asInputStream()).thenReturn(new ByteArrayInputStream(pdfData));
		when(clientMock.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID)).thenReturn(mockFeignResponse);

		final var response = digitalRegisteredLetterIntegration.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getContentType()).hasToString("application/pdf");
		assertThat(response.getHeaders().getFirst("Content-Disposition")).isEqualTo("attachment; filename=receipt.pdf");

		final var outputStream = new ByteArrayOutputStream();
		assertThat(response.getBody()).isNotNull();
		response.getBody().writeTo(outputStream);
		assertThat(outputStream.toByteArray()).isEqualTo(pdfData);

		verify(clientMock).getLetterReceipt(MUNICIPALITY_ID, LETTER_ID);
	}

	@Test
	void getLetterReceipt_streamingIOException() throws Exception {
		final Response mockFeignResponse = mock(Response.class);
		final Response.Body mockBody = mock(Response.Body.class);
		final InputStream mockInputStream = mock(InputStream.class);
		final Map<String, Collection<String>> headers = new HashMap<>();
		headers.put("Content-Type", List.of("application/pdf"));
		headers.put("Content-Disposition", List.of("attachment; filename=receipt.pdf"));

		when(mockFeignResponse.status()).thenReturn(200);
		when(mockFeignResponse.headers()).thenReturn(headers);
		when(mockFeignResponse.body()).thenReturn(mockBody);
		when(mockBody.asInputStream()).thenReturn(mockInputStream);
		when(mockInputStream.transferTo(any())).thenThrow(new IOException("Stream error"));
		when(clientMock.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID)).thenReturn(mockFeignResponse);

		final var response = digitalRegisteredLetterIntegration.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID);
		final var outputStream = new ByteArrayOutputStream();

		assertThat(response.getBody()).isNotNull();
		assertThatThrownBy(() -> response.getBody().writeTo(outputStream))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Could not stream letter receipt: Stream error")
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);

		verify(clientMock).getLetterReceipt(MUNICIPALITY_ID, LETTER_ID);
	}

	@Test
	void getLetterReceipt_missingContentType() {
		final Response mockFeignResponse = mock(Response.class);
		final Map<String, Collection<String>> headers = new HashMap<>();
		headers.put("Content-Disposition", List.of("attachment; filename=receipt.pdf"));

		when(mockFeignResponse.headers()).thenReturn(headers);
		when(clientMock.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID)).thenReturn(mockFeignResponse);

		assertThatThrownBy(() -> digitalRegisteredLetterIntegration.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Missing Content-Type header in letter receipt response")
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);

		verify(clientMock).getLetterReceipt(MUNICIPALITY_ID, LETTER_ID);
	}

	@Test
	void getLetterReceipt_missingContentDisposition() {
		final Response mockFeignResponse = mock(Response.class);
		final Map<String, Collection<String>> headers = new HashMap<>();
		headers.put("Content-Type", List.of("application/pdf"));

		when(mockFeignResponse.headers()).thenReturn(headers);
		when(clientMock.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID)).thenReturn(mockFeignResponse);

		assertThatThrownBy(() -> digitalRegisteredLetterIntegration.getLetterReceipt(MUNICIPALITY_ID, LETTER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Missing Content-Disposition header in letter receipt response")
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);

		verify(clientMock).getLetterReceipt(MUNICIPALITY_ID, LETTER_ID);
	}
}
