package se.sundsvall.postportalservice.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.objectstore.ObjectStoreIntegration;

import static java.util.Optional.ofNullable;

/**
 * Uploads a message's attachments to the object store once, however many recipients the message has.
 * <p>
 * Delivery runs one task per recipient against a single shared {@link MessageEntity}, so an upload placed on the
 * delivery path runs recipients &times; attachments times - a letter to five hundred people with three attachments
 * performs fifteen hundred uploads of the same bytes and leaves fifteen hundred objects behind for the store's whole
 * time to live, where three would do. The id is remembered on the attachment itself, next to the base64 that is
 * already memoised there for the same reason.
 * <p>
 * The upload stays lazy rather than being hoisted above the fan-out. Hoisting looks simpler but moves the failure:
 * the fan-out runs on the request thread after the message has been persisted, so a store that is down would answer
 * the caller with a 502 and leave every recipient stranded at PENDING with nothing left to deliver them. Kept here,
 * a store failure is caught per recipient exactly as it is today, and the next recipient tries the upload again.
 */
@Service
public class AttachmentUploadService {

	private final ObjectStoreIntegration objectStoreIntegration;

	public AttachmentUploadService(final ObjectStoreIntegration objectStoreIntegration) {
		this.objectStoreIntegration = objectStoreIntegration;
	}

	/**
	 * @return the id of each attachment on the message, in order - the order is the contract, since the queue message
	 *         pairs ids to attachments by position
	 */
	public List<String> storeOnce(final MessageEntity messageEntity) {
		return ofNullable(messageEntity.getAttachments()).orElse(List.of()).stream()
			.map(this::storeOnce)
			.toList();
	}

	String storeOnce(final AttachmentEntity attachmentEntity) {
		// Locked for two reasons, both of which matter, and neither of which a bare null check gives. Mutual
		// exclusion, because the delivery pool's threads share this instance and would otherwise all read null and
		// all upload. And visibility: a field written lazily by one pool thread has no happens-before edge to any
		// other, so without the lock a second thread could keep reading null indefinitely. The neighbouring
		// contentString escapes this only because it is written before the executor hands the task over, and
		// Executor.execute is itself a synchronisation point.
		synchronized (attachmentEntity) {
			if (attachmentEntity.getObjectId() == null) {
				attachmentEntity.setObjectId(objectStoreIntegration.store(attachmentEntity));
			}
			return attachmentEntity.getObjectId();
		}
	}
}
