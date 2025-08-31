package co.com.pragma.crediya.utils;

import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Supplier;

public class ValidationHelper {

    public static Mono<Void> validateAll(List<Supplier<Mono<Void>>> validations) {
        return Flux.fromIterable(validations)
                .concatMap(Supplier::get)
                .then();
    }

    public static Mono<Void> validateCondition(boolean condition, ValidationExceptionMessage message) {
        return condition ? Mono.empty() : Mono.error(new ValidationException(message));
    }

    public static Mono<Void> validateBusinessCondition(boolean condition, BusinessExceptionMessage message) {
        return condition ? Mono.empty() : Mono.error(new BusinessException(message));
    }
}



