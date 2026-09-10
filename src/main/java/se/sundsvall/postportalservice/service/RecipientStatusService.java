package se.sundsvall.postportalservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.StatusMessage;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Applies the delivery outcome the messaging service reports back for anything sent over a queue.
 * <p>
 * One service for every channel, because nothing here is channel-specific: the recipient row is the same row whatever
 * carried the message to it. This closes the loop that {@link MessageService#updateRecipient} closes synchronously from
 * the Feign response on the REST path.
 * <p>
 * Because messaging retries a transient failure through its own backoff ladder before giving up, the outcome can arrive
 * minutes after the publish - until then the recipient stays PENDING. Exactly one terminal outcome is published per
 * send request, so there is nothing to wait for beyond it and no timeout to apply.
 * <p>
 * The same outcome can arrive more than once: messaging publishes it, waits for the confirm and only then acks, so a
 * crash in between redelivers the request and produces a duplicate. The recipient id is the correlation key the
 * duplicates are recognised by.
 */
@Service
public class RecipientStatusService {

	private static final Logger LOG = LoggerFactory.getLogger(RecipientStatusService.class);

	// recipient.external_id is VARCHAR(36). A longer value would fail the insert, and a failed insert requeues the
	// outcome until the delivery limit parks it - losing the whole outcome over a traceability field.
	static final int MAX_EXTERNAL_ID_LENGTH = 36;

	private final RecipientRepository recipientRepository;

	public RecipientStatusService(final RecipientRepository recipientRepository) {
		this.recipientRepository = recipientRepository;
	}

	public void handleStatus(final StatusMessage statusMessage) {
		recipientRepository.findById(statusMessage.recipientId())
			.ifPresentOrElse(
				recipientEntity -> applyOutcome(recipientEntity, statusMessage),
				() -> LOG.warn("Received status for unknown recipient with id {}, ignoring it", statusMessage.recipientId()));
	}

	private void applyOutcome(final RecipientEntity recipientEntity, final StatusMessage statusMessage) {
		// SENT is never revised. It is the one state a duplicate can only make less true, and it also lets a genuine
		// outcome correct a recipient this service marked FAILED on an unconfirmed publish.
		if (SENT.equals(recipientEntity.getStatus())) {
			LOG.info("Recipient with id {} is already SENT, ignoring duplicate outcome", recipientEntity.getId());
			return;
		}

		final var status = statusOf(statusMessage);

		LOG.info("Updating recipient with id {}, Status: {}, ExternalId: {}", recipientEntity.getId(), status, statusMessage.externalId());
		recipientEntity.setStatus(status);
		recipientEntity.setStatusDetail(statusMessage.statusDetail());
		ofNullable(statusMessage.externalId())
			.filter(externalId -> isStorable(externalId, recipientEntity.getId()))
			.ifPresent(recipientEntity::setExternalId);
		recipientRepository.save(recipientEntity);
	}

	/** A status we cannot read is a delivery we cannot vouch for, so it counts as a failure. */
	private static String statusOf(final StatusMessage statusMessage) {
		if (isBlank(statusMessage.status())) {
			return FAILED;
		}
		return statusMessage.status();
	}

	private static boolean isStorable(final String externalId, final String recipientId) {
		if (externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
			// The outcome itself is worth more than the id it came with, so record the outcome and drop the id.
			LOG.warn("External id for recipient {} is {} characters and does not fit, leaving it unset", recipientId, externalId.length());
			return false;
		}
		return true;
	}
}
