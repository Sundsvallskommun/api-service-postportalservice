package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
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

		var result = mapper.toEligibilityRequest(partyIds);

		assertThat(result.getPartyIds()).containsExactlyInAnyOrder(partyId1, partyId2, partyId3);
	}

	@Test
	void toLetterRequest_whenNull() {
		assertThat(mapper.toLetterRequest(null, null)).isNull();
	}

	@Test
	void toLetterRequest() {
		var partyId = "123e4567-e89b-12d3-a456-426614174000";
		var department = new DepartmentEntity()
			.withName("Sundsvalls kommun")
			.withOrganizationNumber("12345")
			.withContactInformationEmail("email")
			.withContactInformationPhoneNumber("number")
			.withContactInformationUrl("url")
			.withSupportText("supportText");
		var message = new MessageEntity()
			.withBody("body")
			.withContentType("application/pdf")
			.withSubject("subject")
			.withDepartment(department);
		var recipient = new RecipientEntity()
			.withPartyId(partyId);

		var result = mapper.toLetterRequest(message, recipient);

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
		var department = new DepartmentEntity()
			.withContactInformationEmail("email")
			.withContactInformationPhoneNumber("number")
			.withContactInformationUrl("url")
			.withSupportText("supportText");

		var result = mapper.toSupportInfo(department);

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
		var department = new DepartmentEntity()
			.withName("Sundsvalls kommun")
			.withOrganizationNumber("12345");

		var result = mapper.toOrganization(department);

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
		var blobMock = Mockito.mock(Blob.class);
		var content = "content".getBytes();
		when(blobMock.length()).thenReturn((long) 5);
		when(blobMock.getBytes(1, 5)).thenReturn(content);
		var attachment1 = new AttachmentEntity()
			.withContentType("application/pdf")
			.withFileName("file1.pdf")
			.withContent(blobMock);
		var attachment2 = new AttachmentEntity()
			.withContentType("application/json")
			.withFileName("file2.pdf")
			.withContent(blobMock);

		var result = mapper.toMultipartFiles(List.of(attachment1, attachment2));

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
		var blobMock = Mockito.mock(Blob.class);
		var content = "content".getBytes();
		when(blobMock.length()).thenReturn((long) 5);
		when(blobMock.getBytes(1, 5)).thenReturn(content);
		var attachment = new AttachmentEntity()
			.withContentType("application/pdf")
			.withFileName("file1.pdf")
			.withContent(blobMock);

		var result = mapper.toMultipartFile(attachment);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(attachment.getFileName());
		assertThat(result.getContentType()).isEqualTo(attachment.getContentType());
		assertThat(result.getBytes()).isEqualTo(content);
	}

	@Test
	void createMultipartFile() throws Exception {
		var blobMock = Mockito.mock(Blob.class);
		var content = "content".getBytes();
		when(blobMock.length()).thenReturn((long) 5);
		when(blobMock.getBytes(1, 5)).thenReturn(content);
		var attachment = new AttachmentEntity()
			.withContentType("application/pdf")
			.withFileName("file1.pdf")
			.withContent(blobMock);

		var result = mapper.createMultipartFile(attachment);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(attachment.getFileName());
		assertThat(result.getContentType()).isEqualTo(attachment.getContentType());
		assertThat(result.getBytes()).isEqualTo(content);
	}

}
