package se.sundsvall.postportalservice.integration.employee;

import static org.apache.commons.lang3.math.NumberUtils.toInt;

import java.util.Optional;

public final class EmployeeUtil {

	private static final String ORGANIZATION_DELIMITER = "¤";
	private static final String INFORMATION_DELIMITER = "\\|";

	private EmployeeUtil() {}

	/**
	 * Parses the organization string and returns the department (level 2) if found, otherwise returns null.
	 *
	 * @param  organizationsString the organization string from the Employee API
	 * @return                     A department model object that represents the department (level 2) or null if not found
	 */
	public static Optional<Department> parseOrganizationString(final String organizationsString) {
		final var organizations = Optional.ofNullable(organizationsString)
			.map(string -> string.split(ORGANIZATION_DELIMITER))
			.orElse(new String[0]);

		for (final String organization : organizations) {
			final var orgInfo = organization.split(INFORMATION_DELIMITER);

			if (orgInfo.length != 3) {
				continue;
			}

			final var level = orgInfo[0];
			final var organizationIdentifier = orgInfo[1];
			final var name = orgInfo[2];

			if (toInt(level) == 2) {
				return Optional.of(new Department(organizationIdentifier, name));
			}
		}

		return Optional.empty();
	}

}
