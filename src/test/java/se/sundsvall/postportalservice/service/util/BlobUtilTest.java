package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;

@ExtendWith(MockitoExtension.class)
class BlobUtilTest {

	@Mock
	private EntityManager entityManagerMock;

	@InjectMocks
	private BlobUtil blobUtil;

	@AfterEach
	void ensureNoInteractionsWereMissed() {
		verifyNoMoreInteractions(entityManagerMock);
	}

	@Test
	void getSessionTest() {

		var session = Mockito.mock(Session.class);
		when(entityManagerMock.unwrap(Session.class)).thenReturn(session);

		var result = blobUtil.getSession();

		assertThat(result).isEqualTo(session);
	}

	@Test
	void createBlob_OK() throws IOException {
		var spy = Mockito.spy(blobUtil);
		var multipartFile = Mockito.mock(MultipartFile.class);

		when(multipartFile.getBytes()).thenReturn(new byte[123]);

		var session = Mockito.mock(Session.class);
		when(spy.getSession()).thenReturn(session);

		var lobHelper = Mockito.mock(LobHelper.class);
		when(session.getLobHelper()).thenReturn(lobHelper);

		var blob = Mockito.mock(Blob.class);
		when(lobHelper.createBlob(any(), eq(123L))).thenReturn(blob);

		var result = spy.createBlob(multipartFile);

		assertThat(result).isEqualTo(blob);
		verify(entityManagerMock).unwrap(any());
	}

	@Test
	void createBlob_IOException() throws IOException {
		var spy = Mockito.spy(blobUtil);
		var multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("TestFile.txt");

		when(multipartFile.getBytes()).thenThrow(new IOException("Test exception"));

		assertThatThrownBy(() -> spy.createBlob(multipartFile))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Could not convert file with name [ TestFile.txt ] to database object");
	}

	@Test
	void convertToBlobTest() throws IOException {
		var spy = Mockito.spy(blobUtil);
		var multipartFile = Mockito.mock(MultipartFile.class);

		when(multipartFile.getBytes()).thenReturn(new byte[123]);

		var session = Mockito.mock(Session.class);
		when(spy.getSession()).thenReturn(session);

		var lobHelper = Mockito.mock(LobHelper.class);
		when(session.getLobHelper()).thenReturn(lobHelper);

		var blob = Mockito.mock(Blob.class);
		when(spy.createBlob(multipartFile)).thenReturn(blob);

		var result = spy.convertToBlob(multipartFile);

		assertThat(result).isEqualTo(blob);
		verify(entityManagerMock).unwrap(any());
	}

	@Test
	void convertBlobToBase64StringTest() throws SQLException {
		var blob = Mockito.mock(Blob.class);

		when(blob.getBytes(1, (int) blob.length())).thenReturn("test".getBytes());

		var result = blobUtil.convertBlobToBase64String(blob);

		assertThat(result).isEqualTo("dGVzdA==");
	}

	@Test
	void convertToBlob_SQLException() throws SQLException {
		var blob = Mockito.mock(Blob.class);
		when(blob.length()).thenThrow(new SQLException("Test exception"));

		assertThatThrownBy(() -> blobUtil.convertBlobToBase64String(blob))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Could not convert Blob to Base64 string: Test exception");

	}

}
