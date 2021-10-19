package it.s3sync.web.dto.request.validator;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RegExpValidator implements ConstraintValidator<RegExp, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		try {
			Pattern.compile(value);
			return true;
		} catch (PatternSyntaxException exception) {
			return false;
		}
	}

}
