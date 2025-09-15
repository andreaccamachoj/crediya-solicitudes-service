package co.com.pragma.crediya.model.sqs.gateways;

import co.com.pragma.crediya.model.sqs.CapacidadLambdaRequest;
import reactor.core.publisher.Mono;

public interface CapacidadEventPublisher {
    Mono<String> send(CapacidadLambdaRequest request);
}
