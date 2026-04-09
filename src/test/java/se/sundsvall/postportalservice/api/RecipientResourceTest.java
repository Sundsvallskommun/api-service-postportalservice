package se.sundsvall.postportalservice.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.postportalservice.api.model.RecipientResponse;
import se.sundsvall.postportalservice.service.RecipientService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipientResourceTest {

	@Mock
	private RecipientService recipientServiceMock;

	@InjectMocks
	private RecipientResource recipientResource;

	@Test
	void getRecipientById() {
		final var municipalityId = "2281";
		final var recipientId = "recipientId";
		final var response = new RecipientResponse("partyId", "subject", "body", "text/html",
			"123", "dept", "support", "url", "email", "phone", "joe01doe", List.of());

		when(recipientServiceMock.getRecipient(municipalityId, recipientId)).thenReturn(response);

		final var result = recipientResource.getRecipientById(municipalityId, recipientId);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		verify(recipientServiceMock).getRecipient(municipalityId, recipientId);
	}
}
