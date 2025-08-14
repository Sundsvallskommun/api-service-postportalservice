package se.sundsvall.postportalservice.integration.db.converter;

public enum MessageStatus {
	PENDING,
	AWAITING_FEEDBACK,
	SENT,
	NOT_SENT,
	FAILED,
	NO_CONTACT_SETTINGS_FOUND,
	NO_CONTACT_WANTED
}
