package it.s3sync.web.controller.advice.exception;

import java.util.Arrays;

import it.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;

public class BadRequestException extends RuntimeException {
	
	private static final long serialVersionUID = 934263467604479908L;

	private Iterable<String> placeholders;
	private ErrorMessage errorMessage;
	
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
