package se.sundsvall.postportalservice.integration.esigning;

import generated.se.sundsvall.esigning.StartSigningRequest;
import generated.se.sundsvall.esigning.StartSigningResponse;
import org.springframework.stereotype.Component;

@Component
public class EsigningIntegration {

	private final EsigningClient client;

	public EsigningIntegration(final EsigningClient client) {
		this.client = client;
	}

	public StartSigningResponse createSigning(final String municipalityId, final StartSigningRequest request) {
		return client.createSigning(municipalityId, request);
	}

}
