package se.sundsvall.postportalservice.service.util;

import static org.zalando.problem.Status.BAD_REQUEST;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;

public final class ParseUtil {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private ParseUtil() {}

	public static LetterRequest parseLetterRequest(final String requestString) {
		try {
			return OBJECT_MAPPER.readValue(requestString, LetterRequest.class);
		} catch (JsonProcessingException e) {
			throw Problem.valueOf(BAD_REQUEST, "Couldn't parse letter request");
		}
	}

	public static DigitalRegisteredLetterRequest parseDigitalRegisteredLetterRequest(final String requestString) {
		try {
			return OBJECT_MAPPER.readValue(requestString, DigitalRegisteredLetterRequest.class);
		} catch (JsonProcessingException e) {
			throw Problem.valueOf(BAD_REQUEST, "Couldn't parse digital registered letter request");
		}
	}
}
