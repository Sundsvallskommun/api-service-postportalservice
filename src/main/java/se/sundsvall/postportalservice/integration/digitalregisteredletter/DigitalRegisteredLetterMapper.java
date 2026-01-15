package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

import generated.se.sundsvall.digitalregisteredletter.Device;
import generated.se.sundsvall.digitalregisteredletter.EligibilityRequest;
import generated.se.sundsvall.digitalregisteredletter.LetterRequest;
import generated.se.sundsvall.digitalregisteredletter.LetterStatusRequest;
import generated.se.sundsvall.digitalregisteredletter.Organization;
import generated.se.sundsvall.digitalregisteredletter.SigningInfo;
import generated.se.sundsvall.digitalregisteredletter.StepUp;
import generated.se.sundsvall.digitalregisteredletter.SupportInfo;
import generated.se.sundsvall.digitalregisteredletter.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Component
public class DigitalRegisteredLetterMapper {

	public EligibilityRequest toEligibilityRequest(final List<String> partyIds) {
		return new EligibilityRequest()
			.partyIds(partyIds);
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

	public SupportInfo toSupportInfo(final DepartmentEntity nullableDepartmentEntity) {
		return ofNullable(nullableDepartmentEntity)
			.map(departmentEntity -> new SupportInfo()
				.contactInformationEmail(departmentEntity.getContactInformationEmail())
				.contactInformationUrl(departmentEntity.getContactInformationUrl())
				.contactInformationPhoneNumber(departmentEntity.getContactInformationPhoneNumber())
				.supportText(departmentEntity.getSupportText()))
			.orElse(null);
	}

	public Organization toOrganization(final DepartmentEntity nullableDepartmentEntity) {
		return ofNullable(nullableDepartmentEntity)
			.map(departmentEntity -> new Organization()
				.number(Integer.valueOf(departmentEntity.getOrganizationNumber()))
				.name(departmentEntity.getName()))
			.orElse(null);
	}

	public List<MultipartFile> toMultipartFiles(final List<AttachmentEntity> attachmentEntities) {
		return ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(this::toMultipartFile)
			.toList();
	}

	public MultipartFile toMultipartFile(final AttachmentEntity attachmentEntity) {
		return ofNullable(attachmentEntity)
			.map(throwingFunction(this::createMultipartFile))
			.orElse(null);
	}

	public MultipartFile createMultipartFile(final AttachmentEntity attachmentEntity) throws SQLException {
		return new AttachmentMultipartFile(attachmentEntity.getFileName(), attachmentEntity.getContentType(), attachmentEntity.getContent());
	}

	public SigningInformation toSigningInformation(final SigningInfo nullableSigningInfo) {
		return ofNullable(nullableSigningInfo)
			.map(signingInfo -> new SigningInformation()
				.withStatus(signingInfo.getStatus())
				.withSignedAt(signingInfo.getSigned())
				.withContentKey(signingInfo.getContentKey())
				.withOrderReference(signingInfo.getOrderRef())
				.withSignature(signingInfo.getSignature())
				.withOcspResponse(signingInfo.getOcspResponse())
				.withUser(toUser(signingInfo.getUser()))
				.withDevice(toDevice(signingInfo.getDevice()))
				.withStepUp(toStepUp(signingInfo.getStepUp())))
			.orElse(null);
	}

	public SigningInformation.User toUser(final User nullableUser) {
		return ofNullable(nullableUser)
			.map(user -> new SigningInformation.User()
				.withPersonalIdentityNumber(user.getPersonalIdentityNumber())
				.withName(user.getName())
				.withGivenName(user.getGivenName())
				.withSurname(user.getSurname())).orElse(null);
	}

	public SigningInformation.Device toDevice(final Device device) {
		return Optional.ofNullable(device)
			.map(_ -> new SigningInformation.Device()
				.withIpAddress(device.getIpAddress()))
			.orElse(null);
	}

	public SigningInformation.StepUp toStepUp(final StepUp nullableStepUp) {
		return ofNullable(nullableStepUp)
			.map(stepUp -> new SigningInformation.StepUp()
				.withMrtd(stepUp.getMrtd()))
			.orElse(null);
	}

	public LetterStatusRequest toLetterStatusRequest(final List<String> nullableLetterIds) {
		return ofNullable(nullableLetterIds)
			.map(letterIds -> new LetterStatusRequest()
				.letterIds(letterIds))
			.orElse(null);
	}
}
