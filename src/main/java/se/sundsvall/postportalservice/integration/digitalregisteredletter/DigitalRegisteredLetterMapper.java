package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

import generated.se.sundsvall.digitalregisteredletter.EligibilityRequest;
import generated.se.sundsvall.digitalregisteredletter.LetterRequest;
import generated.se.sundsvall.digitalregisteredletter.Organization;
import generated.se.sundsvall.digitalregisteredletter.SupportInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Component
public class DigitalRegisteredLetterMapper {

	public EligibilityRequest toEligibilityRequest(final List<String> partyIds) {
		return new EligibilityRequest().partyIds(partyIds);
	}

	public LetterRequest toLetterRequest(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}

		return new LetterRequest()
			.body(messageEntity.getBody())
			.contentType(messageEntity.getContentType())
			.subject(messageEntity.getSubject())
			.supportInfo(toSupportInfo(messageEntity.getDepartment()))
			.organization(toOrganization(messageEntity.getDepartment()))
			.partyId(recipientEntity.getPartyId());
	}

	public SupportInfo toSupportInfo(final DepartmentEntity departmentEntity) {
		return Optional.ofNullable(departmentEntity).map(present -> new SupportInfo()
			.contactInformationEmail(departmentEntity.getContactInformationEmail())
			.contactInformationUrl(departmentEntity.getContactInformationUrl())
			.contactInformationPhoneNumber(departmentEntity.getContactInformationPhoneNumber())
			.supportText(departmentEntity.getSupportText()))
			.orElse(null);
	}

	public Organization toOrganization(final DepartmentEntity departmentEntity) {
		return Optional.ofNullable(departmentEntity).map(present -> new Organization()
			.number(Integer.valueOf(departmentEntity.getOrganizationNumber()))
			.name(departmentEntity.getName()))
			.orElse(null);
	}

	public List<MultipartFile> toMultipartFiles(final List<AttachmentEntity> attachmentEntities) {
		return Optional.ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(this::toMultipartFile)
			.toList();
	}

	public MultipartFile toMultipartFile(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity)
			.map(throwingFunction(this::createMultipartFile))
			.orElse(null);
	}

	public MultipartFile createMultipartFile(final AttachmentEntity attachmentEntity) throws Exception {
		return new AttachmentMultipartFile(attachmentEntity.getFileName(), attachmentEntity.getContentType(), attachmentEntity.getContent());
	}

}
