package co.com.pragma.crediya.api.config;

import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static boolean isSwagger(ServerWebExchange exchange) {
        String p = exchange.getRequest().getPath().value();
        return p.startsWith("/swagger-ui")
                || p.startsWith("/v3/api-docs")
                || p.startsWith("/webjars");
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex, ServerWebExchange exchange) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Error de validación");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("code", ex.getCode());
        problemDetail.setProperty("itcCode", ex.getItcCode());
        problemDetail.setProperty("path", exchange.getRequest().getPath().value());
        return problemDetail;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, ServerWebExchange exchange) {
        var status = switch (ex.getCode()) {
            case "BUS0001" -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        var problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle("Error de negocio");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("code", ex.getCode());
        problemDetail.setProperty("itcCode", ex.getItcCode());
        problemDetail.setProperty("path", exchange.getRequest().getPath().value());
        return problemDetail;
    }

    @ExceptionHandler(Throwable.class)
    public ProblemDetail handleAny(Throwable ex, ServerWebExchange exchange) {
        if (isSwagger(exchange)) {
            // Deja que springdoc/recursos estáticos manejen su propia excepción
            if (ex instanceof RuntimeException re) throw re;
            throw new RuntimeException(ex);
        }
        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Falla inesperada");
        pd.setDetail("Ocurrió un error inesperado. Intenta más tarde.");
        pd.setProperty("path", exchange.getRequest().getPath().value());
        return pd;
    }
}

