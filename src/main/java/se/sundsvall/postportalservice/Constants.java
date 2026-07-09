package se.sundsvall.postportalservice;

public final class Constants {

	private Constants() {}

	public static final String ORIGIN = "PostPortalService";
	public static final String FAILED = "FAILED";
	public static final String PENDING = "PENDING";
	public static final String SENT = "SENT";
	public static final String UNDELIVERABLE = "UNDELIVERABLE";
	public static final String INELIGIBLE_MINOR = "INELIGIBLE_MINOR";

	// E-signing case status (mirrors the normalized status from api-service-e-signing). SIGNERAT is terminal.
	public static final String SIGNERAT = "SIGNERAT";
	// Terminal error/cancelled state (Comfact 'withdrawn'/'declined'/'halted' all normalize to FEL in e-signing).
	public static final String FEL = "FEL";

	// E-signing recipient (signatory) status
	public static final String SIGNED = "SIGNED";
	public static final String DECLINED = "DECLINED";
}
