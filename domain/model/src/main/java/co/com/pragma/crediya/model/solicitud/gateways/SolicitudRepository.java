package co.com.pragma.crediya.model.solicitud.gateways;

import co.com.pragma.crediya.model.solicitud.Solicitud;
import reactor.core.publisher.Mono;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud usuario);
}
