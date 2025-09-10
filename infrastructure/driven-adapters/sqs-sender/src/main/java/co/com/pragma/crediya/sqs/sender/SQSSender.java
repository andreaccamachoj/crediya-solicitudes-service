package co.com.pragma.crediya.sqs.sender;

import co.com.pragma.crediya.model.sqs.gateways.NotificacionEventPublisher;
import co.com.pragma.crediya.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements NotificacionEventPublisher {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> send(Object messagePayload) {
        return Mono.fromCallable(() -> {
                    try {
                        return objectMapper.writeValueAsString(messagePayload);
                    } catch (JsonProcessingException e) {
                        log.error("Error serializando el objeto a JSON para SQS", e);
                        throw new RuntimeException("Error de serialización para SQS", e);
                    }
                }).flatMap(this::sendMessageToQueue)
                .doOnError(e -> log.error("Fallo el envío del mensaje a SQS. Payload: {}", messagePayload, e));
    }

    private Mono<String> sendMessageToQueue(String messageBody) {
        return Mono.fromCallable(() -> buildRequest(messageBody))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnSuccess(response -> log.info("Mensaje enviado a SQS con éxito. MessageId: {}", response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
