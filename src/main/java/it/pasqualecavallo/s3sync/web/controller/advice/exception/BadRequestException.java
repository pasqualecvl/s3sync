package it.pasqualecavallo.s3sync.web.controller.advice.exception;

import java.util.Arrays;

import it.pasqualecavallo.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;

public class BadRequestException extends RuntimeException {
	
	Iterable<String> placeholders;
	ErrorMessage errorMessage;
	
	public BadRequestException(ErrorMessage errorMessage, String...placeholders) {
		this.placeholders = Arrays.asList(placeholders);
		this.errorMessage = errorMessage;
	}
	
	public void setPlaceholders(Iterable<String> placeholders) {
		this.placeholders = placeholders;
	}
	
	public Iterable<String> getPlaceholders() {
		return placeholders;
	}
	
	public void setErrorMessage(ErrorMessage errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public ErrorMessage getErrorMessage() {
		return errorMessage;
	}
	
}
