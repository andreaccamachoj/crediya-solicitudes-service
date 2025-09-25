package co.com.pragma.crediya.model.gateway;

import reactor.core.publisher.Mono;

public interface ReactiveTxGateway {
    <T> Mono<T> required(Mono<T> work);
}