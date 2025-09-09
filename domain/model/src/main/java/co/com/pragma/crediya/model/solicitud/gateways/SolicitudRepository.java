package co.com.pragma.crediya.model.solicitud.gateways;

import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.SolicitudDetalle;
import co.com.pragma.crediya.model.solicitud.SolicitudPageRequest;
import co.com.pragma.crediya.model.solicitud.SolicitudPageResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud usuario);
    public Flux<Long> findDistinctIdUsuariosByEstados(List<String> estados, String email);
    public Mono<SolicitudPageResponse<SolicitudDetalle>> findDetallesByEstadosAndUsuariosPaged(
            List<String> estados, List<Long> usuarios, SolicitudPageRequest pageRequest);
    public Flux<SolicitudDetalle> findSolicitudesAprobadasByUsuarios(List<Long> usuarios);
}
