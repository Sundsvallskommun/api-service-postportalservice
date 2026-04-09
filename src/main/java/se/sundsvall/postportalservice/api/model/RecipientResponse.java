package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Recipient data needed for sending a digital registered letter")
public record RecipientResponse(
	@Schema(description = "Party ID of the recipient") String partyId,
	@Schema(description = "Subject of the letter") String subject,
	@Schema(description = "Body of the letter") String body,
	@Schema(description = "Content type of the body, e.g. text/plain or text/html") String contentType,
	@Schema(description = "Organization number") String organizationNumber,
	@Schema(description = "Organization name") String organizationName,
	@Schema(description = "Support text") String supportText,
	@Schema(description = "Contact information URL") String contactInformationUrl,
	@Schema(description = "Contact information email") String contactInformationEmail,
	@Schema(description = "Contact information phone number") String contactInformationPhoneNumber,
	@Schema(description = "Identifier for the x-sent-by header") String identifier,
	@Schema(description = "Attachment metadata") List<AttachmentMetadata> attachments) {

	@Schema(description = "Metadata for an attachment")
	public record AttachmentMetadata(
		@Schema(description = "Attachment ID") String id,
		@Schema(description = "File name") String fileName,
		@Schema(description = "Content type") String contentType) {
	}
}
