package se.sundsvall.postportalservice.service.util;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.allNull;

public final class StringUtil {
	private StringUtil() {}

	public static String calculateFullName(final String firstName, final String lastName) {
		if (allNull(firstName, lastName)) {
			return null;
		}

		return "%s %s".formatted(
			ofNullable(firstName).map(String::trim).orElse(""),
			ofNullable(lastName).map(String::trim).orElse("")).trim();
	}

	/**
	 * Build a recipient display name from a combination of person name and organisation name.
	 *
	 * <ul>
	 * <li>Both organisation name and person name present → {@code "<organisation> (att: <firstName> <lastName>)"}</li>
	 * <li>Only organisation name present → {@code "<organisation>"}</li>
	 * <li>Only person name present → {@code "<firstName> <lastName>"} (via {@link #calculateFullName})</li>
	 * <li>Nothing present → {@code null}</li>
	 * </ul>
	 */
	public static String calculateRecipientName(final String firstName, final String lastName, final String organizationName) {
		final var personName = calculateFullName(firstName, lastName);
		final var hasOrganizationName = organizationName != null && !organizationName.isBlank();
		final var hasPersonName = personName != null && !personName.isBlank();

		if (hasOrganizationName && hasPersonName) {
			return "%s (att: %s)".formatted(organizationName, personName);
		}
		if (hasOrganizationName) {
			return organizationName;
		}
		return personName;
	}
}
