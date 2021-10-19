package it.s3sync.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
public class GenericConfiguration {

	@Bean("sqsJsonMapper")
	public JsonMapper sqsJsonMapper() {
		return new JsonMapper();
	}
}
