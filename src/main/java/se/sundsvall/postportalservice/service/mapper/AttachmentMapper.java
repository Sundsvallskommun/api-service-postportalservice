package se.sundsvall.postportalservice.service.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.service.util.BlobUtil;

import static java.util.Collections.emptyList;
import static se.sundsvall.postportalservice.service.util.BlobUtil.convertBlobToBase64String;

@Component
public final class AttachmentMapper {

	private final BlobUtil blobUtil;

	public AttachmentMapper(final BlobUtil blobUtil) {
		this.blobUtil = blobUtil;
	}

	public List<AttachmentEntity> toAttachmentEntities(final List<MultipartFile> attachments) {
		return Optional.ofNullable(attachments).orElse(emptyList()).stream()
			.map(this::toAttachmentEntity)
			.collect(Collectors.toList());// Mutable list
	}

	public AttachmentEntity toAttachmentEntity(final MultipartFile multipartFile) {
		if (multipartFile == null) {
			return null;
		}
		var blob = blobUtil.convertToBlob(multipartFile);
		var contentString = convertBlobToBase64String(blob);

		return new AttachmentEntity()
			.withFileName(multipartFile.getOriginalFilename())
			.withContentType(multipartFile.getContentType())
			.withContent(blob)
			.withContentString(contentString);
	}

}
