package se.sundsvall.postportalservice.integration.db.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MessageStatusTest {

	@ParameterizedTest
	@MethodSource("fromValueArgumentsProvider")
	void fromValue(final String value, final MessageStatus expected) {
		var result = MessageStatus.fromValue(value);

		assertThat(result).isEqualTo(expected);
	}

	private static Stream<Arguments> fromValueArgumentsProvider() {
		return Stream.of(
			Arguments.of("SENT", MessageStatus.SENT),
			Arguments.of("sent", MessageStatus.SENT),
			Arguments.of("PENDING", MessageStatus.PENDING),
			Arguments.of("pending", MessageStatus.PENDING),
			Arguments.of("not_sent", MessageStatus.NOT_SENT),
			Arguments.of("failed", MessageStatus.FAILED),
			Arguments.of("INELIGIBLE_MINOR", MessageStatus.INELIGIBLE_MINOR),
			Arguments.of("ineligible_minor", MessageStatus.INELIGIBLE_MINOR),
			Arguments.of("unknown", MessageStatus.FAILED),
			Arguments.of(null, MessageStatus.FAILED));
	}

}
