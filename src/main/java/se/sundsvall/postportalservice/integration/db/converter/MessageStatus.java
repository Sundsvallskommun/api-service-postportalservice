package se.sundsvall.postportalservice.integration.db.converter;

public enum MessageStatus {
	PENDING,
	AWAITING_FEEDBACK,
	SENT,
	NOT_SENT,
	FAILED,
	NO_CONTACT_SETTINGS_FOUND,
	NO_CONTACT_WANTED,
	INELIGIBLE_MINOR;

	public static MessageStatus fromValue(final String value) {
		try {
			return MessageStatus.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			// If we don't recognize the value, we default to FAILED.
			return MessageStatus.FAILED;
		}
	}
}
