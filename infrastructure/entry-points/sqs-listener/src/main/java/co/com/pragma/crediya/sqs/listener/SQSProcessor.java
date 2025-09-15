package co.com.pragma.crediya.sqs.listener;

import co.com.pragma.crediya.model.sqs.ResultadoSolicitud;
import co.com.pragma.crediya.usecase.solicitud.SolicitudUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Log4j2
public class SQSProcessor implements Function<Message, Mono<Void>> {

    private final SolicitudUseCase solicitudUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(() -> {
                    String body = message.body();
                    log.info("[SQS] Mensaje recibido: {}", body);

                    return objectMapper.readValue(body, ResultadoSolicitud.class);
                })
                .flatMap(this::procesarResultado)
                .onErrorResume(e -> {
                    log.error("[SQS] Error procesando mensaje", e);
                    return Mono.empty();
                });
    }

    private Mono<Void> procesarResultado(ResultadoSolicitud result) {
        log.info("[SQS] Procesando resultado: idSolicitud={} decision={}",
                result.getIdSolicitud(), result.getDecision());

        return solicitudUseCase.actualizarEstadoConResultado(result)
                .doOnSuccess(r -> log.info("[SQS] Solicitud {} actualizada con éxito", result.getIdSolicitud()))
                .doOnError(e -> log.error("[SQS] Error actualizando solicitud {}", result.getIdSolicitud(), e))
                .then();
    }
}
