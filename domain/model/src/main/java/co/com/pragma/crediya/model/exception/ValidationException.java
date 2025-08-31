package co.com.pragma.crediya.model.exception;

import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ValidationException extends RuntimeException{

    private final ValidationExceptionMessage validationExceptionMessage;
    @Override public String getMessage() { return validationExceptionMessage.getMessage(); }
    public String getCode()             { return validationExceptionMessage.getCode(); }
    public String getItcCode()          { return validationExceptionMessage.getItcCode(); }
}
