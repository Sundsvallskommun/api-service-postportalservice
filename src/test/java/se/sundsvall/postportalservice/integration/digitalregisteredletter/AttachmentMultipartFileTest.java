package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.sql.Blob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentMultipartFileTest {

	@Test
	void constructorAndGetters() throws Exception {
		final var content = new byte[10];
		final var name = "file";
		final var contentType = "application/pdf";

		final var blob = Mockito.mock(Blob.class);
		when(blob.length()).thenReturn(10L);
		when(blob.getBytes(1, 10)).thenReturn(content);

		var attachmentMultipartFile = new AttachmentMultipartFile(name, contentType, blob);

		assertThat(attachmentMultipartFile.getName()).isEqualTo(name);
		assertThat(attachmentMultipartFile.getOriginalFilename()).isEqualTo(name);
		assertThat(attachmentMultipartFile.getContentType()).isEqualTo(contentType);
		assertThat(attachmentMultipartFile.getSize()).isEqualTo(10L);
		assertThat(attachmentMultipartFile.getBytes()).isEqualTo(content);
		assertThat(attachmentMultipartFile.getInputStream()).isNotNull().isInstanceOf(InputStream.class);
		assertThat(attachmentMultipartFile.isEmpty()).isFalse();
	}

}
