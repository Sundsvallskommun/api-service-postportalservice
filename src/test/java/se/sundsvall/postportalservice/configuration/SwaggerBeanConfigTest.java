package se.sundsvall.postportalservice.configuration;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SwaggerBeanConfigTest {

	@Mock
	private HttpMessageConverters.ServerBuilder serverBuilder;

	private final SwaggerBeanConfig configuration = new SwaggerBeanConfig();

	@Test
	void octetStreamIsAddedToJacksonConverter() {
		final ArgumentCaptor<Consumer<HttpMessageConverter<?>>> captor = ArgumentCaptor.captor();

		configuration.configureMessageConverters(serverBuilder);

		verify(serverBuilder).configureMessageConverters(captor.capture());

		final var jacksonConverter = new JacksonJsonHttpMessageConverter();
		captor.getValue().accept(jacksonConverter);

		assertThat(jacksonConverter.getSupportedMediaTypes()).contains(new MediaType("application", "octet-stream"));
	}

	@Test
	void nonJacksonConverterIsLeftUnchanged() {
		final ArgumentCaptor<Consumer<HttpMessageConverter<?>>> captor = ArgumentCaptor.captor();

		configuration.configureMessageConverters(serverBuilder);

		verify(serverBuilder).configureMessageConverters(captor.capture());

		// The instanceof guard means a non-Jackson converter is left untouched (no exception, no media-type change).
		final HttpMessageConverter<?> otherConverter = mock(HttpMessageConverter.class);
		captor.getValue().accept(otherConverter);
	}
}
