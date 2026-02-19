package se.sundsvall.postportalservice.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.util.PrecheckUtil;

import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.ORGANIZATION_NUMBER;

/**
 * Service for checking digital mailbox status.
 */
@Service
public class MailboxStatusService {

	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final MessagingIntegration messagingIntegration;

	public MailboxStatusService(
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final MessagingIntegration messagingIntegration) {
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.messagingIntegration = messagingIntegration;
	}

	/**
	 * Checks mailbox status for given partyIds.
	 *
	 * @param  municipalityId the municipalityId
	 * @param  partyIds       list of partyIds to check
	 * @return                mailbox status with reachable and unreachable partyIds
	 */
	public MailboxStatus checkMailboxStatus(final String municipalityId, final List<String> partyIds) {
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var mailboxes = messagingIntegration.precheckMailboxes(municipalityId, settingsMap.get(ORGANIZATION_NUMBER), partyIds);

		final var reachable = PrecheckUtil.filterReachableMailboxes(mailboxes);
		final var unreachableWithReason = PrecheckUtil.filterUnreachableMailboxesWithReason(mailboxes);

		return new MailboxStatus(reachable, unreachableWithReason);
	}

	/**
	 * Record for mailbox status results, so we know which ones are reachable and not.
	 *
	 * @param reachable             partyIds with reachable digital mailboxes
	 * @param unreachableWithReason unreachable mailboxes with reason information
	 */
	public record MailboxStatus(
		List<String> reachable,
		List<PrecheckUtil.UnreachableMailbox> unreachableWithReason) {
		// Convenience method for precheckLegalIds flow
		public List<String> unreachable() {
			return unreachableWithReason.stream()
				.map(PrecheckUtil.UnreachableMailbox::partyId)
				.toList();
		}
	}
}
