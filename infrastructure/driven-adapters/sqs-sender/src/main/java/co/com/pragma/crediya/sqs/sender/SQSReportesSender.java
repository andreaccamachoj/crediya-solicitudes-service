package co.com.pragma.crediya.sqs.sender;

import co.com.pragma.crediya.model.sqs.ReportesSolicitudesAprobadas;
import co.com.pragma.crediya.model.sqs.SolicitudesAprobadas;
import co.com.pragma.crediya.model.sqs.gateways.AprobacionEventPublisher;
import co.com.pragma.crediya.sqs.sender.config.properties.SQSReportesProperties;
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
public class SQSReportesSender implements AprobacionEventPublisher {
    private final SQSReportesProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> send(ReportesSolicitudesAprobadas request) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(request))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException("Error de serialización para SQS solicitudes aprobadas", e))
                .flatMap(this::sendMessageToQueue)
                .doOnError(e -> log.error("Fallo el envío del mensaje a SQS (solicitudes aprobadas). Payload: {}", request, e));
    }

    private Mono<String> sendMessageToQueue(String messageBody) {
        return Mono.fromCallable(() -> buildRequest(messageBody))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnSuccess(response -> log.info("[Capacidad] Mensaje enviado con éxito. MessageId: {}", response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
