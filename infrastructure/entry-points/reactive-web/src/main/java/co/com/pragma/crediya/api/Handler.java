package co.com.pragma.crediya.api;

import co.com.pragma.crediya.api.dto.request.CrearSolicitudRequest;
import co.com.pragma.crediya.api.mapper.SolicitudMapper;
import co.com.pragma.crediya.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {
    private final SolicitudUseCase useCase;
    private final SolicitudMapper mapper;

    public Mono<ServerResponse> listenSaveSolicitud(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CrearSolicitudRequest.class)
                .map(mapper::toDomain)
                .flatMap(useCase::crearSolicitud)
                .map(mapper::toResponse)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

}
