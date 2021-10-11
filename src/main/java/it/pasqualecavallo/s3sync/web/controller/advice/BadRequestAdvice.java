package it.pasqualecavallo.s3sync.web.controller.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import it.pasqualecavallo.s3sync.web.controller.advice.exception.BadRequestException;
import it.pasqualecavallo.s3sync.web.dto.response.RestBaseResponse;

@RestControllerAdvice
public class BadRequestAdvice {

	@ExceptionHandler(value = { BadRequestException.class })
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public RestBaseResponse adviceBadRequest(BadRequestException exception, HttpServletRequest request) {
		RestBaseResponse response = new RestBaseResponse();
		response.setError(exception.getErrorMessage(), exception.getPlaceholders());
		return response;
	}

}
