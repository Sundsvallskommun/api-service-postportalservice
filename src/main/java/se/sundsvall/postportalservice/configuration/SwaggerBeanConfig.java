package se.sundsvall.postportalservice.configuration;

import java.util.ArrayList;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerBeanConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(final HttpMessageConverters.ServerBuilder builder) {
		builder.configureMessageConverters(converter -> {
			if (converter instanceof final JacksonJsonHttpMessageConverter jacksonConverter) {
				final var supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
				supportedMediaTypes.add(new MediaType("application", "octet-stream"));
				jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
			}
		});
	}
}
