package co.com.pragma.crediya.model.exception;

import co.com.pragma.crediya.model.exception.message.TechnicalExceptionMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TechnicalException extends RuntimeException{

    private final TechnicalExceptionMessage technicalExceptionMessage;

    @Override
    public String getMessage() { return technicalExceptionMessage.getMessage(); }
    public String getCode()             { return technicalExceptionMessage.getCode(); }
    public String getItcCode()          { return technicalExceptionMessage.getItcCode(); }
    public String getDescription()      { return technicalExceptionMessage.getDescription(); }
}
