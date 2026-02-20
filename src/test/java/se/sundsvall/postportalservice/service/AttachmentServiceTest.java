package se.sundsvall.postportalservice.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.dao.AttachmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@InjectMocks
	private AttachmentService attachmentService;

	@AfterEach
	void ensureNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(attachmentRepositoryMock);
	}

	@Test
	void getAttachmentById() {
		var attachmentId = "attachmentId";
		var attachmentEntityMock = Mockito.mock(AttachmentEntity.class);

		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.of(attachmentEntityMock));

		var result = attachmentService.getAttachmentById(attachmentId);

		assertThat(result).isNotNull().isEqualTo(attachmentEntityMock);
		verify(attachmentRepositoryMock).findById(attachmentId);
		verifyNoMoreInteractions(attachmentEntityMock);
	}

	@Test
	void getAttachmentsById_notFound() {
		var attachmentId = "attachmentId";

		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> attachmentService.getAttachmentById(attachmentId))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Attachment with id '%s' was not found".formatted(attachmentId));

		verify(attachmentRepositoryMock).findById(attachmentId);
	}

	@Test
	void getAttachmentData() throws IOException, SQLException {
		var attachmentId = "attachmentId";
		var attachmentEntityMock = Mockito.mock(AttachmentEntity.class);
		var fileName = "fileName";
		var contentType = "application/pdf";

		var blob = Mockito.mock(Blob.class);
		var payload = "hello".getBytes(StandardCharsets.UTF_8);
		when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(payload));

		when(attachmentEntityMock.getFileName()).thenReturn(fileName);
		when(attachmentEntityMock.getContentType()).thenReturn(contentType);
		when(attachmentEntityMock.getContent()).thenReturn(blob);
		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.of(attachmentEntityMock));

		var result = attachmentService.getAttachmentData(MUNICIPALITY_ID, attachmentId);

		// Trigger the streaming
		var out = new ByteArrayOutputStream();
		result.contentStream().writeTo(out);

		assertThat(result).isNotNull();
		assertThat(result.contentDisposition().getFilename()).isEqualTo(fileName);
		assertThat(result.contentType()).isEqualTo(MediaType.parseMediaType(contentType));

		// Assert that the content is the expected one
		assertThat(out.toByteArray()).isEqualTo(payload);

		verify(blob).getBinaryStream();
		verify(attachmentEntityMock).getContent();
		verify(attachmentEntityMock).getFileName();
		verify(attachmentEntityMock).getContentType();
		verify(attachmentRepositoryMock).findById(attachmentId);

		verifyNoMoreInteractions(attachmentEntityMock, blob);
	}

	@Test
	void getAttachmentData_noContent() {
		var attachmentId = "attachmentId";
		var attachmentEntityMock = Mockito.mock(AttachmentEntity.class);
		var fileName = "fileName";
		var contentType = "application/pdf";

		when(attachmentEntityMock.getId()).thenReturn(attachmentId);
		when(attachmentEntityMock.getFileName()).thenReturn(fileName);
		when(attachmentEntityMock.getContentType()).thenReturn(contentType);
		when(attachmentEntityMock.getContent()).thenReturn(null);
		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.of(attachmentEntityMock));

		assertThatThrownBy(() -> {
			var result = attachmentService.getAttachmentData(MUNICIPALITY_ID, attachmentId);
			// Trigger the streaming
			var out = new ByteArrayOutputStream();
			result.contentStream().writeTo(out);
		})
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Internal Server Error: No content for attachment with id '%s'".formatted(attachmentId));

		verify(attachmentEntityMock).getContent();
		verify(attachmentRepositoryMock).findById(attachmentId);

		verifyNoMoreInteractions(attachmentEntityMock);
	}

	@Test
	void getAttachmentData_sqlException() throws SQLException {
		var attachmentId = "attachmentId";
		var attachmentEntityMock = Mockito.mock(AttachmentEntity.class);
		var fileName = "fileName";
		var contentType = "application/pdf";

		var blob = Mockito.mock(Blob.class);
		when(blob.getBinaryStream()).thenThrow(SQLException.class);

		when(attachmentEntityMock.getId()).thenReturn(attachmentId);
		when(attachmentEntityMock.getFileName()).thenReturn(fileName);
		when(attachmentEntityMock.getContentType()).thenReturn(contentType);
		when(attachmentEntityMock.getContent()).thenReturn(blob);
		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.of(attachmentEntityMock));

		assertThatThrownBy(() -> {
			var result = attachmentService.getAttachmentData(MUNICIPALITY_ID, attachmentId);
			// Trigger the streaming
			var out = new ByteArrayOutputStream();
			result.contentStream().writeTo(out);
		})
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Internal Server Error: Failed to open content stream for attachment with id '%s'".formatted(attachmentId));

		verify(blob).getBinaryStream();
		verify(attachmentEntityMock).getContent();
		verify(attachmentRepositoryMock).findById(attachmentId);

		verifyNoMoreInteractions(attachmentEntityMock, blob);
	}

	@Test
	void getAttachmentData_ioException() throws SQLException {
		var attachmentId = "attachmentId";
		var attachmentEntityMock = Mockito.mock(AttachmentEntity.class);
		var fileName = "fileName";
		var contentType = "application/pdf";

		var blob = Mockito.mock(Blob.class);
		var payload = "hello".getBytes(StandardCharsets.UTF_8);
		when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(payload));

		when(attachmentEntityMock.getId()).thenReturn(attachmentId);
		when(attachmentEntityMock.getFileName()).thenReturn(fileName);
		when(attachmentEntityMock.getContentType()).thenReturn(contentType);
		when(attachmentEntityMock.getContent()).thenReturn(blob);
		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.of(attachmentEntityMock));

		assertThatThrownBy(() -> {
			var result = attachmentService.getAttachmentData(MUNICIPALITY_ID, attachmentId);
			// Trigger the streaming with an output stream that throws IOException when writing
			result.contentStream().writeTo(new ByteArrayOutputStream() {
				@Override
				public void write(byte[] b, int off, int len) {
					try {
						throw new IOException("Simulated IO error");
					} catch (IOException e) {
						throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to stream content for attachment with id '%s'".formatted(attachmentEntityMock.getId()));
					}
				}
			});
		})
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Internal Server Error: Failed to stream content for attachment with id '%s'".formatted(attachmentEntityMock.getId()));

		verify(blob).getBinaryStream();
		verify(attachmentEntityMock).getContent();
		verify(attachmentRepositoryMock).findById(attachmentId);

		verifyNoMoreInteractions(attachmentEntityMock, blob);
	}

	@Test
	void getAttachmentData_InvalidMediaType() throws IOException, SQLException {
		var attachmentId = "attachmentId";
		var attachmentEntityMock = Mockito.mock(AttachmentEntity.class);
		var fileName = "fileName";
		var contentType = "invalid/content type";

		var blob = Mockito.mock(Blob.class);
		var payload = "hello".getBytes(StandardCharsets.UTF_8);

		when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(payload));
		when(attachmentEntityMock.getFileName()).thenReturn(fileName);
		when(attachmentEntityMock.getContentType()).thenReturn(contentType);
		when(attachmentEntityMock.getContent()).thenReturn(blob);
		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.of(attachmentEntityMock));

		var result = attachmentService.getAttachmentData(MUNICIPALITY_ID, attachmentId);

		// Trigger the streaming
		var out = new ByteArrayOutputStream();
		result.contentStream().writeTo(out);

		assertThat(result).isNotNull();
		assertThat(result.contentDisposition().getFilename()).isEqualTo(fileName);

		// Assert that an invalid content type defaults to APPLICATION_OCTET_STREAM
		assertThat(result.contentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);

		// Assert that the content is the expected one
		assertThat(out.toByteArray()).isEqualTo(payload);

		verify(blob).getBinaryStream();
		verify(attachmentEntityMock).getContent();
		verify(attachmentEntityMock).getFileName();
		verify(attachmentEntityMock).getContentType();
		verify(attachmentRepositoryMock).findById(attachmentId);

		verifyNoMoreInteractions(attachmentEntityMock, blob);
	}

	@Test
	void getAttachmentData_notFound() {
		var attachmentId = "attachmentId";

		when(attachmentRepositoryMock.findById(attachmentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> attachmentService.getAttachmentData(MUNICIPALITY_ID, attachmentId))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Attachment with id '%s' was not found".formatted(attachmentId));

		verify(attachmentRepositoryMock).findById(attachmentId);
	}

}
