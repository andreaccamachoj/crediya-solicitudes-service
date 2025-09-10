package co.com.pragma.crediya.model.sqs.gateways;

import reactor.core.publisher.Mono;

public interface NotificacionEventPublisher {
    public Mono<String> send(Object messagePayload);
}

