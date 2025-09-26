package se.sundsvall.postportalservice.service.util;

import se.sundsvall.dept44.support.Identifier;

public class IdentifierUtil {

	private IdentifierUtil() {}

	public static String getIdentifierHeaderValue(final String userName) {
		return Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue(userName)
			.toHeaderValue();
	}
}
