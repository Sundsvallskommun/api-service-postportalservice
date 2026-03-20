package se.sundsvall.postportalservice.service.util;

import java.util.Map;

public class CallbackEmailUtil {

	public static final String SNAILMAIL_METHOD_KEY = "snailmail_method";
	public static final String SNAILMAIL_METHOD_VALUE = "Callback_Email";

	private static final String SNAILMAIL_CALLBACK_EMAIL_KEY = "callback_email";
	private static final String SNAILMAIL_CALLBACK_SUBJECT_KEY = "callback_email_subject";
	private static final String SNAILMAIL_CALLBACK_BODY_KEY = "callback_email_body_base64";

	private CallbackEmailUtil() {}

	public static String getCallbackEmail(final Map<String, String> settingsMap) {
		return settingsMap.get(SNAILMAIL_CALLBACK_EMAIL_KEY);
	}

	public static String getCallbackEmailSubject(final Map<String, String> settingsMap) {
		return settingsMap.get(SNAILMAIL_CALLBACK_SUBJECT_KEY);
	}

	public static String getEmailBody(final Map<String, String> settingsMap) {
		return settingsMap.get(SNAILMAIL_CALLBACK_BODY_KEY);
	}
}
