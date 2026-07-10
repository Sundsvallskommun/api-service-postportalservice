package se.sundsvall.postportalservice;

public final class Constants {

	private Constants() {}

	public static final String ORIGIN = "PostPortalService";
	public static final String FAILED = "FAILED";
	public static final String PENDING = "PENDING";
	public static final String SENT = "SENT";
	public static final String UNDELIVERABLE = "UNDELIVERABLE";
	public static final String INELIGIBLE_MINOR = "INELIGIBLE_MINOR";

	// E-signing recipient (signatory) and case status. SIGNED is terminal for signing cases.
	public static final String SIGNED = "SIGNED";
	public static final String DECLINED = "DECLINED";
}
