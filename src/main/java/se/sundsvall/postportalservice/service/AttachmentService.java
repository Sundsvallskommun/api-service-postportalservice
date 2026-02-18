package se.sundsvall.postportalservice.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.dao.AttachmentRepository;

import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;

@Service
public class AttachmentService {

	private final AttachmentRepository attachmentRepository;

	public AttachmentService(final AttachmentRepository attachmentRepository) {
		this.attachmentRepository = attachmentRepository;
	}

	public AttachmentData getAttachmentData(final String municipalityId, final String attachmentId) {
		var attachmentEntity = getAttachmentById(attachmentId);

		final StreamingResponseBody contentStream = output -> writeAttachmentContent(attachmentEntity, output);
		final var contentType = parseMediaType(attachmentEntity);

		var contentDisposition = ContentDisposition.attachment()
			.filename(ofNullable(attachmentEntity.getFileName()).orElse("attachment"), StandardCharsets.UTF_8)
			.build();

		return new AttachmentData(contentDisposition, contentType, contentStream);
	}

	public AttachmentEntity getAttachmentById(final String attachmentId) {
		return attachmentRepository.findById(attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Attachment with id '%s' was not found".formatted(attachmentId)));
	}

	private MediaType parseMediaType(final AttachmentEntity attachmentEntity) {
		return ofNullable(attachmentEntity.getContentType())
			.map(type -> {
				try {
					return MediaType.parseMediaType(type);
				} catch (InvalidMediaTypeException e) {
					return APPLICATION_OCTET_STREAM;
				}
			})
			.orElse(APPLICATION_OCTET_STREAM);
	}

	private void writeAttachmentContent(final AttachmentEntity attachmentEntity, final OutputStream output) {
		var content = ofNullable(attachmentEntity.getContent())
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "No content for attachment with id '%s'".formatted(attachmentEntity.getId())));

		try (var input = content.getBinaryStream()) {
			StreamUtils.copy(input, output);
		} catch (SQLException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to open content stream for attachment with id '%s'".formatted(attachmentEntity.getId()));
		} catch (IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to stream content for attachment with id '%s'".formatted(attachmentEntity.getId()));
		}
	}

	public record AttachmentData(ContentDisposition contentDisposition, MediaType contentType, StreamingResponseBody contentStream) {
	}
}
