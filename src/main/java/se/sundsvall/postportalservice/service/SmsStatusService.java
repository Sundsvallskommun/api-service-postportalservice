package se.sundsvall.postportalservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsStatusMessage;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Applies the delivery outcome that the messaging service reports back for an SMS sent over the queue.
 * <p>
 * This closes the loop that {@link MessageService#updateRecipient} used to close synchronously from the Feign response.
 * Because messaging retries a transient failure through its own backoff ladder before giving up, the outcome can arrive
 * minutes after the publish - until then the recipient stays PENDING. Exactly one terminal outcome is published per
 * send request, so there is nothing to wait for beyond it and no timeout to apply.
 * <p>
 * The same outcome can arrive more than once: messaging publishes it, waits for the confirm and only then acks, so a
 * crash in between redelivers the request and produces a duplicate. The recipient id is the correlation key the
 * duplicates are recognised by.
 */
@Service
public class SmsStatusService {

	private static final Logger LOG = LoggerFactory.getLogger(SmsStatusService.class);

	// recipient.external_id is VARCHAR(36). A longer value would fail the insert, and a failed insert requeues the
	// outcome until the delivery limit parks it - losing the whole outcome over a traceability field.
	static final int MAX_EXTERNAL_ID_LENGTH = 36;

	private final RecipientRepository recipientRepository;

	public SmsStatusService(final RecipientRepository recipientRepository) {
		this.recipientRepository = recipientRepository;
	}

	private static boolean isStorable(final String externalId, final String recipientId) {
		if (externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
			// The outcome itself is worth more than the id it came with, so record the outcome and drop the id.
			LOG.warn("External id for recipient {} is {} characters and does not fit, leaving it unset", recipientId, externalId.length());
			return false;
		}
		return true;
	}

	public void handleSmsStatus(final SmsStatusMessage smsStatusMessage) {
		recipientRepository.findById(smsStatusMessage.recipientId())
			.ifPresentOrElse(recipientEntity -> {
				// SENT is never revised. It is the one state a duplicate can only make less true, and it also lets a
				// genuine outcome correct a recipient this service marked FAILED on an unconfirmed publish.
				if (SENT.equals(recipientEntity.getStatus())) {
					LOG.info("Recipient with id {} is already SENT, ignoring duplicate outcome", recipientEntity.getId());
					return;
				}

				// A status we cannot read is a delivery we cannot vouch for, so it counts as a failure.
				final var status = isBlank(smsStatusMessage.status()) ? FAILED : smsStatusMessage.status();

				LOG.info("Updating recipient with id {}, Status: {}, ExternalId: {}", recipientEntity.getId(), status, smsStatusMessage.externalId());
				recipientEntity.setStatus(status);
				recipientEntity.setStatusDetail(smsStatusMessage.statusDetail());
				ofNullable(smsStatusMessage.externalId())
					.filter(externalId -> isStorable(externalId, recipientEntity.getId()))
					.ifPresent(recipientEntity::setExternalId);
				recipientRepository.save(recipientEntity);
			},
				() -> LOG.warn("Received SMS status for unknown recipient with id {}, ignoring it", smsStatusMessage.recipientId()));
	}
}
