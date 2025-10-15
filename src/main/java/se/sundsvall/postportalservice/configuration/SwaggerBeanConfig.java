package se.sundsvall.postportalservice.configuration;

import java.util.ArrayList;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class SwaggerBeanConfig {

	public SwaggerBeanConfig(final MappingJackson2HttpMessageConverter converter) {
		final var supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
		supportedMediaTypes.add(new MediaType("application", "octet-stream"));
		converter.setSupportedMediaTypes(supportedMediaTypes);
	}
}
