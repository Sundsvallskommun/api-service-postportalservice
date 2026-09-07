package se.sundsvall.postportalservice.integration.objectstore;

import java.sql.Blob;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.objectstore.configuration.ObjectStoreProperties;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

/**
 * Uploads an attachment so that it can travel as a reference rather than as content.
 * <p>
 * The object id is minted here and is the only thing that goes onto the queue. What that buys is not disk but the
 * absence of a copy: the bytes are no longer serialized into the queue message, replicated across the quorum queue's
 * three nodes, parked in a wait queue for every retry and kept in the parking lot afterwards.
 */
@Component
public class ObjectStoreIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(ObjectStoreIntegration.class);

	private final ObjectStoreClient client;
	private final ObjectStoreProperties properties;

	public ObjectStoreIntegration(final ObjectStoreClient client, final ObjectStoreProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	/**
	 * @return the id the attachment was stored under
	 */
	public String store(final AttachmentEntity attachmentEntity) {
		final var objectId = UUID.randomUUID().toString();
		final var contentType = defaultIfBlank(attachmentEntity.getContentType(), APPLICATION_OCTET_STREAM_VALUE);
		final var expiresAt = OffsetDateTime.now().plus(Duration.parse(properties.timeToLive())).format(ISO_OFFSET_DATE_TIME);

		client.storeObject(
			properties.bucket(),
			objectId,
			expiresAt,
			contentType,
			"attachment; filename=\"%s\"".formatted(attachmentEntity.getFileName()),
			toBytes(attachmentEntity.getContent(), attachmentEntity.getFileName()));

		LOG.info("Stored attachment {} as object {}", attachmentEntity.getFileName(), objectId);

		return objectId;
	}

	private static byte[] toBytes(final Blob blob, final String fileName) {
		if (blob == null) {
			throw Problem.valueOf(BAD_GATEWAY, "No content for attachment %s, nothing to store".formatted(fileName));
		}

		try {
			return blob.getBytes(1, (int) blob.length());
		} catch (final SQLException e) {
			throw Problem.valueOf(BAD_GATEWAY, "Could not read attachment %s: %s".formatted(fileName, e.getMessage()));
		}
	}
}
