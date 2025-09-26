package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.service.util.IdentifierUtil.getIdentifierHeaderValue;

import java.util.List;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Component
public class DigitalRegisteredLetterIntegration {

	private final DigitalRegisteredLetterClient client;
	private final DigitalRegisteredLetterMapper mapper;

	public DigitalRegisteredLetterIntegration(final DigitalRegisteredLetterClient client, final DigitalRegisteredLetterMapper mapper) {
		this.client = client;
		this.mapper = mapper;
	}

	/**
	 * Takes a list of partyIds and checks their Kivra eligibility, returning a list of eligible partyIds.
	 *
	 * @param  municipalityId the municipality id
	 * @param  partyIds       the party ids to check
	 * @return                a list of eligible partyIds
	 */
	public List<String> checkKivraEligibility(final String municipalityId, final List<String> partyIds) {
		final var request = mapper.toEligibilityRequest(partyIds);
		return client.checkKivraEligibility(municipalityId, request);
	}

	public boolean getAllLetters(final String municipalityId) {
		// TODO: Figure out how this should be implemented, this returns a paginated list of all letters.
		// Not just the letters sent by PostPortalService..
		return true;
	}

	public boolean getLetterById(final String municipalityId, final String letterId) {
		// Used to fetch status for a specific letter.
		// TODO: Implement, this should update the status of the requested letter in the postportalservice
		return true;
	}

	public void sendLetter(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		try {
			final var request = mapper.toLetterRequest(messageEntity, recipientEntity);
			final var multipartFiles = mapper.toMultipartFiles(messageEntity.getAttachments());
			final var letter = client.sendLetter(getIdentifierHeaderValue(messageEntity.getUser().getName()),
				messageEntity.getMunicipalityId(),
				request,
				multipartFiles);
			recipientEntity.setExternalId(letter.getId());
			recipientEntity.setStatus(letter.getStatus());
		} catch (Exception e) {
			recipientEntity.setStatus(FAILED);
			recipientEntity.setStatusDetail(e.getMessage());
		}
	}

}
