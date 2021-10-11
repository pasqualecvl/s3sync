package it.pasqualecavallo.s3sync.web.controller.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import it.pasqualecavallo.s3sync.web.controller.advice.exception.InternalServerErrorException;
import it.pasqualecavallo.s3sync.web.dto.response.RestBaseResponse;

@RestControllerAdvice
public class InternalServerErrorAdvice {

	@ExceptionHandler(value = { InternalServerErrorException.class })
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public RestBaseResponse adviceBadRequest(InternalServerErrorException exception, HttpServletRequest request) {
		RestBaseResponse response = new RestBaseResponse();
		response.setError(exception.getErrorMessage(), exception.getPlaceholders());
		return response;
	}

}
