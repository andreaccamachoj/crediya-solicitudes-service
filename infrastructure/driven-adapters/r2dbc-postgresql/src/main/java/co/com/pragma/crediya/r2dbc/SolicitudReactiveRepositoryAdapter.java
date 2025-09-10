package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.SolicitudDetalle;
import co.com.pragma.crediya.model.solicitud.SolicitudPageRequest;
import co.com.pragma.crediya.model.solicitud.SolicitudPageResponse;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.r2dbc.entity.SolicitudEntity;
import co.com.pragma.crediya.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.crediya.r2dbc.mapper.SolicitudDetalleMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.List;

@Repository
@Transactional
public class SolicitudReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Solicitud,
        SolicitudEntity,
        BigInteger,
        SolicitudReactiveRepository
        > implements SolicitudRepository {

    private final SolicitudDetalleMapper detalleMapper;
    private static final Logger log = LoggerFactory.getLogger(SolicitudReactiveRepositoryAdapter.class);

    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository, ObjectMapper mapper, SolicitudDetalleMapper detalleMapper) {
        super(repository, mapper, d -> mapper.map(d, Solicitud.class));
        this.detalleMapper = detalleMapper;
    }

    @Override
    public Mono<Solicitud> save(Solicitud solicitud) {
        log.info("Intentando guardar Solicitud: {}", solicitud);
        return super.save(solicitud)
                .doOnSuccess(saved -> log.info("Solicitud guardada exitosamente: {}", saved))
                .doOnError(e -> log.error("Error al guardar Solicitud: {}", solicitud, e));
    }

    @Override
    public Flux<Long> findDistinctIdUsuariosByEstados(List<String> estados, String email) {
        log.info("Buscando usuarios distintos por estados: {}", estados);
        return repository.findDistinctIdUsuariosByEstadosByFilter(estados, email)
                .doOnNext(id -> log.debug("Usuario encontrado con ID: {}", id))
                .doOnComplete(() -> log.info("Finalizada búsqueda de usuarios por estados: {}", estados))
                .doOnError(e -> log.error("Error al buscar usuarios por estados: {}", estados, e));
    }

    @Override
    public Mono<SolicitudPageResponse<SolicitudDetalle>> findDetallesByEstadosAndUsuariosPaged(
            List<String> estados, List<Long> usuarios, SolicitudPageRequest pageRequest) {

        log.info("Consultando detalles de solicitudes. Estados: {}, Usuarios: {}, PageRequest: {}",
                estados, usuarios, pageRequest);

        Mono<Long> total = repository.countByEstadosAndUsuarios(estados, usuarios);

        Flux<SolicitudDetalle> content = repository.findDetallesByEstadosAndUsuarios(estados, usuarios)
                .skip(pageRequest.offset())
                .take(pageRequest.getSize())
                .doOnNext(detalle -> log.debug("Detalle encontrado: {}", detalle));

        return total.zipWith(content.collectList(),
                        (totalElements, list) -> SolicitudPageResponse.<SolicitudDetalle>builder()
                                .content(list)
                                .totalElements(totalElements)
                                .page(pageRequest.getPage())
                                .size(pageRequest.getSize())
                                .build()
                ).doOnSuccess(resp -> log.info("Página construida con {} elementos totales", resp.getTotalElements()))
                .doOnError(e -> log.error("Error consultando detalles de solicitudes", e));
    }

    @Override
    public Flux<SolicitudDetalle> findSolicitudesAprobadasByUsuarios(List<Long> usuarios) {
        log.info("Buscando solicitudes aprobadas para usuarios: {}", usuarios);
        return repository.findSolicitudesAprobadasByUsuarios(usuarios)
                .doOnNext(detalle -> log.debug("Solicitud aprobada encontrada: {}", detalle))
                .doOnComplete(() -> log.info("Finalizada búsqueda de solicitudes aprobadas para usuarios: {}", usuarios))
                .doOnError(e -> log.error("Error al buscar solicitudes aprobadas para usuarios: {}", usuarios, e));
    }

    @Override
    public Mono<Integer> updateEstado(Long idSolicitud, Long idEstado) {
        return repository.updateEstado(idSolicitud, idEstado)
                .doOnSubscribe(s -> log.info("[DB] updateEstado sol={} -> estado={}", idSolicitud, idEstado))
                .doOnNext(r -> log.debug("[DB] rowsAffected={}", r))
                .doOnError(e -> log.error("[DB] updateEstado error sol={}", idSolicitud, e));
    }

    @Override
    public Mono<SolicitudDetalle> findDetallesByIdSolicitud(Long idSolicitud) {
        return repository.findDetallesByIdSolicitud(idSolicitud)
                .doOnSubscribe(s -> log.info("[DB] findById({})", idSolicitud))
                .doOnNext(s -> log.debug("[DB] found={}", s))
                .doOnError(e -> log.error("[DB] findById error id={}", idSolicitud, e));
    }

    @Override
    public Mono<Solicitud> findById(Long idSolicitud) {
        return repository.findById(BigInteger.valueOf(idSolicitud))
                .map(entity -> mapper.map(entity, Solicitud.class))
                .doOnSubscribe(s -> log.info("[DB] findById({})", idSolicitud))
                .doOnNext(s -> log.debug("[DB] found={}", s))
                .doOnError(e -> log.error("[DB] findById error id={}", idSolicitud, e));
    }


}
