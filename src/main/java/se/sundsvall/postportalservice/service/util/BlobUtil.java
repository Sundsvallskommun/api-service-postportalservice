package se.sundsvall.postportalservice.service.util;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.util.Base64;
import java.util.Optional;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

@Component
public class BlobUtil {

	private final EntityManager entityManager;

	public BlobUtil(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Blob convertToBlob(final MultipartFile multipartFile) {
		return Optional.ofNullable(multipartFile)
			.map(throwingFunction(this::createBlob))
			.orElse(null);
	}

	Session getSession() {
		return entityManager.unwrap(Session.class);
	}

	Blob createBlob(final MultipartFile multipartFile) {
		try {
			var fileBytes = multipartFile.getBytes();
			var inputStream = new ByteArrayInputStream(fileBytes);
			return getSession().getLobHelper().createBlob(inputStream, fileBytes.length);
		} catch (Exception ignored) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not convert file with name [ %s ] to database object".formatted(multipartFile.getOriginalFilename()));
		}
	}

	/**
	 * Converts a Blob to a Base64 encoded string.
	 *
	 * @param  blob the Blob to convert
	 * @return      the Base64 encoded string representation of the Blob
	 */
	public static String convertBlobToBase64String(final Blob blob) {
		try {
			var bytes = blob.getBytes(1, (int) blob.length());
			return Base64.getEncoder().encodeToString(bytes);
		} catch (Exception exception) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not convert Blob to Base64 string: " + exception.getMessage());
		}
	}
}
