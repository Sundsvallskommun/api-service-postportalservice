package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import generated.se.sundsvall.digitalregisteredletter.Device;
import generated.se.sundsvall.digitalregisteredletter.SigningInfo;
import generated.se.sundsvall.digitalregisteredletter.StepUp;
import generated.se.sundsvall.digitalregisteredletter.User;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.assertj.core.api.iterable.ThrowingExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalRegisteredLetterMapperTest {

	@InjectMocks
	private DigitalRegisteredLetterMapper mapper;

	@Test
	void toEligibilityRequest() {
		final var partyId1 = "123e4567-e89b-12d3-a456-426614174001";
		final var partyId2 = "123e4567-e89b-12d3-a456-426614174002";
		final var partyId3 = "123e4567-e89b-12d3-a456-426614174003";
		final var partyIds = List.of(partyId1, partyId2, partyId3);

		final var result = mapper.toEligibilityRequest(partyIds);

		assertThat(result.getPartyIds()).containsExactlyInAnyOrder(partyId1, partyId2, partyId3);
	}

	@Test
	void toLetterRequest_whenNull() {
		assertThat(mapper.toLetterRequest(null, null)).isNull();
	}

	@Test
	void toLetterRequest() {
		final var partyId = "123e4567-e89b-12d3-a456-426614174000";
		final var department = new DepartmentEntity()
			.withName("Sundsvalls kommun")
			.withOrganizationNumber("12345")
			.withContactInformationEmail("email")
			.withContactInformationPhoneNumber("number")
			.withContactInformationUrl("url")
			.withSupportText("supportText");
		final var message = new MessageEntity()
			.withBody("body")
			.withContentType("application/pdf")
			.withSubject("subject")
			.withDepartment(department);
		final var recipient = new RecipientEntity()
			.withPartyId(partyId);

		final var result = mapper.toLetterRequest(message, recipient);

		assertThat(result).isNotNull().satisfies(letterRequest -> {
			assertThat(letterRequest.getBody()).isEqualTo(message.getBody());
			assertThat(letterRequest.getSubject()).isEqualTo(message.getSubject());
			assertThat(letterRequest.getContentType()).isEqualTo(message.getContentType());
			assertThat(letterRequest.getPartyId()).isEqualTo(recipient.getPartyId());
			assertThat(letterRequest.getSupportInfo()).satisfies(supportInfo -> {
				assertThat(supportInfo.getContactInformationEmail()).isEqualTo(department.getContactInformationEmail());
				assertThat(supportInfo.getContactInformationPhoneNumber()).isEqualTo(department.getContactInformationPhoneNumber());
				assertThat(supportInfo.getContactInformationUrl()).isEqualTo(department.getContactInformationUrl());
				assertThat(supportInfo.getSupportText()).isEqualTo(department.getSupportText());
			});
			assertThat(letterRequest.getOrganization()).satisfies(organization -> {
				assertThat(organization.getName()).isEqualTo(department.getName());
				assertThat(organization.getNumber()).isEqualTo(Integer.valueOf(department.getOrganizationNumber()));
			});
		});
	}

	@Test
	void toSupportInfo_whenNull() {
		assertThat(mapper.toSupportInfo(null)).isNull();
	}

	@Test
	void toSupportInfo() {
		final var department = new DepartmentEntity()
			.withContactInformationEmail("email")
			.withContactInformationPhoneNumber("number")
			.withContactInformationUrl("url")
			.withSupportText("supportText");

		final var result = mapper.toSupportInfo(department);

		assertThat(result).isNotNull().satisfies(supportInfo -> {
			assertThat(supportInfo.getContactInformationEmail()).isEqualTo(department.getContactInformationEmail());
			assertThat(supportInfo.getContactInformationPhoneNumber()).isEqualTo(department.getContactInformationPhoneNumber());
			assertThat(supportInfo.getContactInformationUrl()).isEqualTo(department.getContactInformationUrl());
			assertThat(supportInfo.getSupportText()).isEqualTo(department.getSupportText());
		});
	}

	@Test
	void toOrganization_whenNull() {
		assertThat(mapper.toOrganization(null)).isNull();
	}

	@Test
	void toOrganization() {
		final var department = new DepartmentEntity()
			.withName("Sundsvalls kommun")
			.withOrganizationNumber("12345");

		final var result = mapper.toOrganization(department);

		assertThat(result).isNotNull().satisfies(organization -> {
			assertThat(organization.getName()).isEqualTo(department.getName());
			assertThat(organization.getNumber()).isEqualTo(Integer.valueOf(department.getOrganizationNumber()));
		});
	}

	@Test
	void toMultipartFiles_whenNull() {
		assertThat(mapper.toMultipartFiles(null)).isEmpty();
	}

	@Test
	void toMultipartFiles() throws SQLException {
		final var blobMock = Mockito.mock(Blob.class);
		final var content = "content".getBytes();
		when(blobMock.length()).thenReturn((long) 5);
		when(blobMock.getBytes(1, 5)).thenReturn(content);
		final var attachment1 = new AttachmentEntity()
			.withContentType("application/pdf")
			.withFileName("file1.pdf")
			.withContent(blobMock);
		final var attachment2 = new AttachmentEntity()
			.withContentType("application/json")
			.withFileName("file2.pdf")
			.withContent(blobMock);

		final var result = mapper.toMultipartFiles(List.of(attachment1, attachment2));

		assertThat(result).isNotNull().hasSize(2)
			.extracting(MultipartFile::getName)
			.containsExactlyInAnyOrder("file1.pdf", "file2.pdf");
		assertThat(result).extracting(MultipartFile::getContentType)
			.containsExactlyInAnyOrder("application/pdf", "application/json");
		assertThat(result).extracting((ThrowingExtractor<? super MultipartFile, byte[], ? extends Exception>) MultipartFile::getBytes)
			.containsExactlyInAnyOrder(content, content);
	}

	@Test
	void toMultipartFile_whenNull() {
		assertThat(mapper.toMultipartFile(null)).isNull();
	}

	@Test
	void toMultipartFile() throws SQLException, IOException {
		final var blobMock = Mockito.mock(Blob.class);
		final var content = "content".getBytes();
		when(blobMock.length()).thenReturn((long) 5);
		when(blobMock.getBytes(1, 5)).thenReturn(content);
		final var attachment = new AttachmentEntity()
			.withContentType("application/pdf")
			.withFileName("file1.pdf")
			.withContent(blobMock);

		final var result = mapper.toMultipartFile(attachment);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(attachment.getFileName());
		assertThat(result.getContentType()).isEqualTo(attachment.getContentType());
		assertThat(result.getBytes()).isEqualTo(content);
	}

	@Test
	void createMultipartFile() throws Exception {
		final var blobMock = Mockito.mock(Blob.class);
		final var content = "content".getBytes();
		when(blobMock.length()).thenReturn((long) 5);
		when(blobMock.getBytes(1, 5)).thenReturn(content);
		final var attachment = new AttachmentEntity()
			.withContentType("application/pdf")
			.withFileName("file1.pdf")
			.withContent(blobMock);

		final var result = mapper.createMultipartFile(attachment);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(attachment.getFileName());
		assertThat(result.getContentType()).isEqualTo(attachment.getContentType());
		assertThat(result.getBytes()).isEqualTo(content);
	}

	@Test
	void toLetterStatusRequest() {
		final var ids = List.of("id1", "id2", "id3");

		final var result = mapper.toLetterStatusRequest(ids);

		assertThat(result).isNotNull();
		assertThat(result.getLetterIds()).isEqualTo(ids);
	}

	@Test
	void toLetterStatusRequest_whenNull() {
		assertThat(mapper.toLetterStatusRequest(null)).isNull();
	}

	@Test
	void toSigningInformation_whenNull() {
		assertThat(mapper.toSigningInformation(null)).isNull();
	}

	@Test
	void toSigningInformation() {
		final var status = "COMPLETED";
		final var signed = OffsetDateTime.now();
		final var contentKey = "contentKey";
		final var orderRef = "orderRef";
		final var signature = "signature";
		final var ocspResponse = "ocspResponse";
		final var personalIdentityNumber = "199001011234";
		final var name = "Test User";
		final var givenName = "Test";
		final var surname = "User";
		final var ipAddress = "192.168.1.1";
		final var mrtd = true;

		final var signingInfo = new SigningInfo()
			.status(status)
			.signed(signed)
			.contentKey(contentKey)
			.orderRef(orderRef)
			.signature(signature)
			.ocspResponse(ocspResponse)
			.user(new User()
				.personalIdentityNumber(personalIdentityNumber)
				.name(name)
				.givenName(givenName)
				.surname(surname))
			.device(new Device().ipAddress(ipAddress))
			.stepUp(new StepUp().mrtd(mrtd));

		final var result = mapper.toSigningInformation(signingInfo);

		assertThat(result).isNotNull().satisfies(info -> {
			assertThat(info.getStatus()).isEqualTo(status);
			assertThat(info.getSignedAt()).isEqualTo(signed);
			assertThat(info.getContentKey()).isEqualTo(contentKey);
			assertThat(info.getOrderReference()).isEqualTo(orderRef);
			assertThat(info.getSignature()).isEqualTo(signature);
			assertThat(info.getOcspResponse()).isEqualTo(ocspResponse);
			assertThat(info.getUser()).satisfies(user -> {
				assertThat(user.getPersonalIdentityNumber()).isEqualTo(personalIdentityNumber);
				assertThat(user.getName()).isEqualTo(name);
				assertThat(user.getGivenName()).isEqualTo(givenName);
				assertThat(user.getSurname()).isEqualTo(surname);
			});
			assertThat(info.getDevice()).satisfies(device -> {
				assertThat(device.getIpAddress()).isEqualTo(ipAddress);
			});
			assertThat(info.getStepUp()).satisfies(stepUp -> {
				assertThat(stepUp.getMrtd()).isEqualTo(mrtd);
			});
		});
	}
}
