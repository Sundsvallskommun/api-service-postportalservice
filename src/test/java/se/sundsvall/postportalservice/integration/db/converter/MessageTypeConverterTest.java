package se.sundsvall.postportalservice.integration.db.converter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeConverterTest {

	private final MessageTypeConverter messageTypeConverter = new MessageTypeConverter();

	@ParameterizedTest
	@EnumSource(MessageType.class)
	void convertToDatabaseColumn(final MessageType messageType) {
		var result = messageTypeConverter.convertToDatabaseColumn(messageType);
		assertThat(result).isEqualTo(messageType.toString());
	}

	@ParameterizedTest
	@EnumSource(MessageType.class)
	void convertToEntityAttribute(final MessageType messageType) {
		var result = messageTypeConverter.convertToEntityAttribute(messageType.toString());
		assertThat(result).isEqualTo(messageType);
	}
}
