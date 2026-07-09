package se.sundsvall.postportalservice.integration.esigning;

import generated.se.sundsvall.esigning.Initiator;
import generated.se.sundsvall.esigning.Message;
import generated.se.sundsvall.esigning.Signatory;
import generated.se.sundsvall.esigning.SigningDocument;
import generated.se.sundsvall.esigning.StartSigningRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.api.model.ESigningRequest;
import se.sundsvall.postportalservice.api.model.ESigningSignatory;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;

import static java.util.Collections.emptyList;

@Component
public final class EsigningMapper {

	/**
	 * Builds the provider-neutral e-signing request. The message id is passed as {@code customerReference} so the provider
	 * echoes it back in every callback, letting Postportalservice correlate the case. The initiator is derived from the
	 * sending department, and the primary document plus any attachments are sent inline as base64.
	 */
	public StartSigningRequest toStartSigningRequest(final MessageEntity message, final ESigningRequest request, final AttachmentEntity document, final List<AttachmentEntity> attachments) {
		return new StartSigningRequest()
			.customerReference(message.getId())
			.language(request.getLanguage())
			.expires(request.getExpires())
			.document(toSigningDocument(document))
			.attachments(toSigningDocuments(attachments))
			.initiator(toInitiator(message.getDepartment()))
			.notificationMessage(new Message().subject(message.getSubject()).body(message.getBody()))
			.signatories(toSignatories(request.getSignatories()));
	}

	List<SigningDocument> toSigningDocuments(final List<AttachmentEntity> attachments) {
		return Optional.ofNullable(attachments).orElseGet(List::of).stream()
			.map(this::toSigningDocument)
			.toList();
	}

	SigningDocument toSigningDocument(final AttachmentEntity attachment) {
		return new SigningDocument()
			.fileName(attachment.getFileName())
			.mimeType(attachment.getContentType())
			.content(attachment.getContentString());
	}

	Initiator toInitiator(final DepartmentEntity department) {
		return Optional.ofNullable(department)
			.map(d -> new Initiator()
				.name(d.getName())
				.organization(d.getName())
				.email(d.getContactInformationEmail()))
			.orElseGet(Initiator::new);
	}

	Set<Signatory> toSignatories(final java.util.List<ESigningSignatory> signatories) {
		return Optional.ofNullable(signatories).orElse(emptyList()).stream()
			.map(this::toSignatory)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	Signatory toSignatory(final ESigningSignatory signatory) {
		return new Signatory()
			.name(signatory.getName())
			.partyId(signatory.getPartyId())
			.email(signatory.getEmail());
	}
}
