package se.sundsvall.postportalservice.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Blob;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.model.Attachments;
import se.sundsvall.postportalservice.service.util.BlobUtil;

@ExtendWith(MockitoExtension.class)
class AttachmentMapperTest {

	@Mock
	private BlobUtil blobUtil;

	@InjectMocks
	private AttachmentMapper attachmentMapper;

	@AfterEach
	void ensureNoInteractionsWereMissed() {
		verifyNoMoreInteractions(blobUtil);
	}

	@Test
	void toAttachmentEntities() {
		var blob = Mockito.mock(Blob.class);
		var multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("file");
		when(multipartFile.getContentType()).thenReturn("application/pdf");
		when(blobUtil.convertToBlob(multipartFile)).thenReturn(blob);

		var files = List.of(multipartFile, multipartFile);
		var attachments = Attachments.create()
			.withFiles(files);

		var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);

		assertThat(attachmentEntities).isNotNull().isNotEmpty().allSatisfy(attachment -> {
			assertThat(attachment.getFileName()).isEqualTo("file");
			assertThat(attachment.getContentType()).isEqualTo("application/pdf");
			assertThat(attachment.getContent()).isEqualTo(blob);
		});
	}

	@Test
	void toAttachmentEntity() {
		var blob = Mockito.mock(Blob.class);
		var multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("file");
		when(multipartFile.getContentType()).thenReturn("application/pdf");
		when(blobUtil.convertToBlob(multipartFile)).thenReturn(blob);

		var attachmentEntity = attachmentMapper.toAttachmentEntity(multipartFile);

		assertThat(attachmentEntity).isNotNull();
		assertThat(attachmentEntity.getFileName()).isEqualTo("file");
		assertThat(attachmentEntity.getContentType()).isEqualTo("application/pdf");
		assertThat(attachmentEntity.getContent()).isEqualTo(blob);
	}

}
