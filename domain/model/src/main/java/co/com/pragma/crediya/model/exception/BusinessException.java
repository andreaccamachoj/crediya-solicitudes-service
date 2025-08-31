package co.com.pragma.crediya.model.exception;

import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BusinessException extends  RuntimeException{

    private final BusinessExceptionMessage businessExceptionMessage;

    @Override public String getMessage() { return businessExceptionMessage.getMessage(); }
    public String getCode()             { return businessExceptionMessage.getCode(); }
    public String getItcCode()          { return businessExceptionMessage.getItcCode(); }
    public String getDescription()      { return businessExceptionMessage.getDescription(); }
}
