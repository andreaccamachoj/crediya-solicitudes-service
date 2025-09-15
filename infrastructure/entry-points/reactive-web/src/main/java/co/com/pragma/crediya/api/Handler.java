package co.com.pragma.crediya.api;

import co.com.pragma.crediya.api.dto.request.CalcularCapacidadRequest;
import co.com.pragma.crediya.api.dto.request.CrearSolicitudRequest;
import co.com.pragma.crediya.api.dto.request.SolicitudCambioEstadoRequest;
import co.com.pragma.crediya.api.mapper.SolicitudMapper;
import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.SolicitudPageRequest;
import co.com.pragma.crediya.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class Handler {
    private final SolicitudUseCase useCase;
    private final SolicitudMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(Handler.class);


    public Mono<UsuarioAutenticado> principal(ServerRequest request) {
        return Mono.deferContextual(ctx -> {
            UsuarioAutenticado ctxUser = ctx.getOrDefault("AUTH_USER", null);
            log.info("[PRINCIPAL] Context -> AUTH_USER={}", ctxUser);
            if (ctxUser != null) {
                return Mono.just(ctxUser);
            }
            UsuarioAutenticado attrUser = request.exchange().getAttribute("authUser");
            log.info("[PRINCIPAL] Exchange attribute -> authUser={}", attrUser);
            if (attrUser != null) {
                return Mono.just(attrUser);
            }
            log.warn("[PRINCIPAL] No se encontró usuario en Context ni en Exchange Attributes");
            return Mono.error(new BusinessException(BusinessExceptionMessage.USER_NOT_FOUND));
        });
    }


    public Mono<ServerResponse> listenSaveSolicitud(ServerRequest serverRequest) {
        return Mono.zip(
                        serverRequest.bodyToMono(CrearSolicitudRequest.class),
                        principal(serverRequest)
                ).flatMap(tuple -> {
                    CrearSolicitudRequest req = tuple.getT1();
                    UsuarioAutenticado auth = tuple.getT2();
                    Solicitud domain = mapper.toDomain(req);
                    return useCase.crearSolicitud(domain, auth);
                }).map(mapper::toResponse)
                .flatMap(resp -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> listarSolicitudes(ServerRequest request) {

        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String email = request.queryParam("email").orElse(null);

        log.info("[SOLICITUDES] GET /api/v1/solicitud/listar?page={}&size={}", page, size);

        SolicitudPageRequest pageRequest = new SolicitudPageRequest(page, size);

        return Mono.zip(
                        Mono.just(pageRequest),
                        principal(request)
                ).flatMap(tuple -> {
                    SolicitudPageRequest pr = tuple.getT1();
                    UsuarioAutenticado auth = tuple.getT2();

                    return useCase.listarSolicitudesPendientes(pr, auth, email);
                })
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> actualizarEstado(ServerRequest req) {
        Mono<SolicitudCambioEstadoRequest> bodyMono = req.bodyToMono(SolicitudCambioEstadoRequest.class);
        Mono<UsuarioAutenticado> authMono = principal(req);

        return Mono.zip(bodyMono, authMono)
                .doOnSubscribe(s -> log.info("[PUT] /api/v1/solicitud cambio de estado - iniciando"))
                .flatMap(t -> useCase.actualizarEstadoSolicitud(mapper.toDomainFromCambioEstadoRequest(t.getT1()) , t.getT2()))
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
                .doOnSuccess(r -> log.info("[PUT] /api/v1/solicitud OK"))
                .onErrorResume(e -> {
                    log.error("[PUT] unexpected error", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "Ocurrió un problema procesando la solicitud"));
                });
    }


    public Mono<ServerResponse> enviarSolicitudACapacidad(ServerRequest request) {
        return request.bodyToMono(CalcularCapacidadRequest.class)
                .map(CalcularCapacidadRequest::idSolicitud)
                .flatMap(useCase::prepararMensajeCapacidadParaLambda)
                .flatMap(messageId -> ServerResponse
                        .accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("messageId", messageId)));
    }
}