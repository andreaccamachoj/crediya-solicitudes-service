package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.enums.EstadoSolicitud;
import co.com.pragma.crediya.enums.RolNombre;
import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.TechnicalException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.TechnicalExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.mapper.SolicitudCapacidadMapper;
import co.com.pragma.crediya.model.solicitud.*;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.sqs.NuevaSolicitud;
import co.com.pragma.crediya.model.sqs.ReportesSolicitudesAprobadas;
import co.com.pragma.crediya.model.sqs.ResultadoSolicitud;
import co.com.pragma.crediya.model.sqs.SolicitudesAprobadas;
import co.com.pragma.crediya.model.sqs.gateways.AprobacionEventPublisher;
import co.com.pragma.crediya.model.sqs.gateways.CapacidadEventPublisher;
import co.com.pragma.crediya.model.sqs.gateways.NotificacionEventPublisher;
import co.com.pragma.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.UsuarioDemografico;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import co.com.pragma.crediya.utils.ValidationHelper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static co.com.pragma.crediya.model.mapper.SolicitudCapacidadMapper.toCapacidadLambdaRequest;
import static co.com.pragma.crediya.model.mapper.SolicitudCapacidadMapper.toNuevaSolicitud;
import static co.com.pragma.crediya.usecase.solicitud.mapper.SolicitudRevisionResponseMapper.buildResponse;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadosRepository estadoRepository;
    private final UsuarioGateway usuarioGateway;
    private final NotificacionEventPublisher notificacionEventPublisher;
    private final CapacidadEventPublisher capacidadEventPublisher;
    private final AprobacionEventPublisher aprobacionEventPublisher;

    private final List<String> estados = List.of("PENDIENTE DE REVISIÓN", "RECHAZADO", "REVISION MANUAL");

    private static final Long ID_ESTADO_PENDIENTE_REVISION = 1L;

    public Mono<Solicitud> crearSolicitud(Solicitud solicitud, UsuarioAutenticado auth) {
        return Mono.just(solicitud)
                .flatMap(s -> validarNegocio(s, auth))
                .flatMap(s -> procesarValidacionesAsincronasYGuardar(s, auth));
    }

    private Mono<Solicitud> validarNegocio(Solicitud s, UsuarioAutenticado auth) {
        if (!RolNombre.USUARIO.getNombre().equalsIgnoreCase(auth.getNombreRol())) {
            return Mono.error(new BusinessException(BusinessExceptionMessage.ROL_NOT_FOUND));
        }

        return ValidationHelper.validateAll(List.of(
                () -> ValidationHelper.validateCondition(s.getMonto() != null,
                        ValidationExceptionMessage.AMOUNT_REQUIRED),
                () -> ValidationHelper.validateCondition(s.getMonto() != null && s.getMonto() > 0.0,
                        ValidationExceptionMessage.AMOUNT_INVALID),
                () -> ValidationHelper.validateCondition(s.getPlazo() != null && s.getPlazo() > 0,
                        ValidationExceptionMessage.TERM_INVALID),
                () -> ValidationHelper.validateCondition(s.getIdTipoPrestamo() != null,
                        ValidationExceptionMessage.LOAN_TYPE_REQUIRED)
        )).thenReturn(s);
    }

    private Mono<Solicitud> procesarValidacionesAsincronasYGuardar(Solicitud solicitud, UsuarioAutenticado auth) {

        Mono<Boolean> validarTipoPrestamo = tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.LOAN_TYPE_NOT_FOUND)));

        Mono<Boolean> validarEstado = estadoRepository.existsById(ID_ESTADO_PENDIENTE_REVISION)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.STATE_NOT_FOUND)));

        Mono<Usuario> obtenerUsuario = usuarioGateway.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.USER_NOT_FOUND)))
                .onErrorResume(ex -> !(ex instanceof BusinessException),
                        ex -> Mono.error(new BusinessException(BusinessExceptionMessage.USER_SERVICE_ERROR)));

        return Mono.zip(validarTipoPrestamo, validarEstado, obtenerUsuario)
                .flatMap(tuple -> {
                    Usuario usuario = tuple.getT3();

                    if (!usuario.getIdUsuario().equals(auth.getIdUsuario())) {
                        return Mono.error(new BusinessException(BusinessExceptionMessage.USER_IDENTITY_MISMATCH));
                    }

                    Solicitud solicitudParaGuardar = solicitud.toBuilder()
                            .idEstado(ID_ESTADO_PENDIENTE_REVISION)
                            .idUsuario(usuario.getIdUsuario())
                            .build();

                    return solicitudRepository.save(solicitudParaGuardar)
                            .flatMap(saved ->
                                    tipoPrestamoRepository.isValidacionAutomaticaEnabled(saved.getIdTipoPrestamo())
                                            .defaultIfEmpty(false)
                                            .flatMap(validAuto ->
                                                    Boolean.TRUE.equals(validAuto)
                                                            ? prepararMensajeCapacidadParaLambda(saved.getIdSolicitud()).thenReturn(saved)
                                                            : Mono.just(saved)
                                            )
                            );
                })
                .onErrorMap(e -> (e instanceof BusinessException || e instanceof ValidationException) ? e
                        : new BusinessException(BusinessExceptionMessage.UNEXPECTED_ERROR));
    }

    public Mono<SolicitudPageResponse<SolicitudRevisionResponse>> listarSolicitudesPendientes(SolicitudPageRequest pageRequest, UsuarioAutenticado auth, String email) {
        if (!RolNombre.ASESOR.getNombre().equalsIgnoreCase(auth.getNombreRol())) {
            return Mono.error(new BusinessException(BusinessExceptionMessage.ROL_NOT_ASESOR));
        }
        return solicitudRepository.findDistinctIdUsuariosByEstados(estados, email)
                .collect(Collectors.toSet())
                .flatMap(userIds -> {
                    if (userIds.isEmpty()) {
                        return Mono.just(SolicitudPageResponse.<SolicitudRevisionResponse>builder()
                                .content(List.of())
                                .totalElements(0)
                                .page(pageRequest.getPage())
                                .size(pageRequest.getSize())
                                .build());
                    }

                    Mono<Map<Long, UsuarioDemografico>> usuariosMono =
                            usuarioGateway.findAllByIds(new ArrayList<>(userIds))
                                    .collectMap(UsuarioDemografico::idUsuario, u -> u);

                    Mono<SolicitudPageResponse<SolicitudDetalle>> solicitudesMono =
                            solicitudRepository.findDetallesByEstadosAndUsuariosPaged(estados, new ArrayList<>(userIds), pageRequest);

                    Mono<Map<Long, Double>> deudasMono =
                            calcularDeudaMensualAprobadaPorUsuarios(new ArrayList<>(userIds));

                    return Mono.zip(usuariosMono, solicitudesMono, deudasMono)
                            .map(tuple -> buildResponse(tuple.getT1(), tuple.getT2(), tuple.getT3()));
                });
    }

    private Mono<Map<Long, Double>> calcularDeudaMensualAprobadaPorUsuarios(List<Long> usuarios) {
        return solicitudRepository.findSolicitudesAprobadasByUsuarios(usuarios)
                .filter(d -> d.getIdusuario() != null)
                .groupBy(SolicitudDetalle::getIdusuario)
                .flatMap(groupedFlux -> groupedFlux
                        .map(this::calcularCuotaMensual)
                        .reduce(0.0, Double::sum)
                        .map(total -> Map.entry(groupedFlux.key(), total)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private Double calcularCuotaMensual(SolicitudDetalle solicitud) {
        if (solicitud.getPlazo() == null || solicitud.getPlazo() <= 0) return 0.0;

        double monto = Optional.ofNullable(solicitud.getMonto()).orElse(0.0);
        double tasa = Optional.ofNullable(solicitud.getTasainteres()).orElse(0.0);
        int plazo = solicitud.getPlazo();

        if (tasa > 0) {
            double r = tasa / 100.0 / 12.0;
            return monto * r / (1 - Math.pow(1 + r, -plazo));
        }
        return monto / plazo;
    }

    public Mono<SolicitudDetalle> actualizarEstadoSolicitud(
            SolicitudEstado req,
            UsuarioAutenticado auth
    ) {
        return validarSolicitudUpdate(req, auth)
                .flatMap(validReq -> {
                    Mono<Solicitud> solicitudMono = solicitudRepository.findById(validReq.getIdSolicitud())
                            .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.REQUEST_NOT_FOUND)));

                    Mono<Long> idEstadoMono = estadoRepository.findIdByNombre(validReq.getEstado().trim().toUpperCase())
                            .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.STATE_NOT_FOUND)));

                    return Mono.zip(solicitudMono, idEstadoMono)
                            .flatMap(tuple -> {
                                Long idEstadoDestino = tuple.getT2();
                                return solicitudRepository.updateEstado(validReq.getIdSolicitud(), idEstadoDestino)
                                        .then(
                                                solicitudRepository.findDetallesByIdSolicitud(validReq.getIdSolicitud())
                                                        .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.REQUEST_NOT_FOUND)))
                                                        .flatMap(detalleActualizado ->
                                                                notificacionEventPublisher.send(detalleActualizado)
                                                                        .then(
                                                                                enviarEventoAprobacionSiCorresponde(
                                                                                        validReq.getEstado(),
                                                                                        detalleActualizado.getIdsolicitud(),
                                                                                        detalleActualizado.getIdusuario(),
                                                                                        detalleActualizado.getMonto()
                                                                                ).thenReturn(detalleActualizado)
                                                                        )
                                                        )
                                        );
                            });
                })
                .onErrorMap(e -> (e instanceof BusinessException || e instanceof ValidationException) ? e
                        : new BusinessException(BusinessExceptionMessage.UNEXPECTED_ERROR));
    }

    private Mono<SolicitudEstado> validarSolicitudUpdate(SolicitudEstado req, UsuarioAutenticado auth) {
        if (!RolNombre.ASESOR.getNombre().equalsIgnoreCase(auth.getNombreRol())) {
            return Mono.error(new BusinessException(BusinessExceptionMessage.ROL_NOT_FOUND));
        }

        return ValidationHelper.validateAll(List.of(
                () -> ValidationHelper.validateCondition(req.getIdSolicitud() != null,
                        ValidationExceptionMessage.STATE_REQUIRED),
                () -> ValidationHelper.validateCondition(req.getEstado() != null,
                        ValidationExceptionMessage.STATE_REQUIRED)
        )).thenReturn(req);
    }

    public Mono<String> prepararMensajeCapacidadParaLambda(Long idSolicitud) {
        return solicitudRepository.findDetallesByIdSolicitud(idSolicitud)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.REQUEST_NOT_FOUND)))
                .flatMap(detalle -> {
                    Long idUsuario = detalle.getIdusuario();

                    // 1) Usuario
                    Mono<UsuarioDemografico> usuarioMono = usuarioGateway.findAllByIds(List.of(idUsuario))
                            .next()
                            .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.USER_NOT_FOUND)))
                            .onErrorMap(ex -> !(ex instanceof BusinessException),
                                    ex -> new TechnicalException(TechnicalExceptionMessage.USER_SERVICE_ERROR));

                    // 2) Préstamos aprobados
                    Mono<List<SolicitudesAprobadas>> activosMono = solicitudRepository
                            .findSolicitudesAprobadasByUsuarios(List.of(idUsuario))
                            .filter(sd -> sd.getIdsolicitud() != null)
                            .map(SolicitudCapacidadMapper::toSolicitudAprobada)
                            .collectList();

                    // 3) Nueva solicitud
                    NuevaSolicitud nuevoPrestamo = toNuevaSolicitud(detalle);

                    // 4) Arma payload
                    return Mono.zip(usuarioMono, activosMono)
                            .map(tuple -> toCapacidadLambdaRequest(
                                    idSolicitud,
                                    tuple.getT1(),
                                    nuevoPrestamo,
                                    tuple.getT2()
                            ));
                })
                .flatMap(capacidadEventPublisher::send)
                .onErrorMap(e -> {
                    if (e instanceof BusinessException) return e;
                    if (e instanceof ValidationException) return e;
                    if (e instanceof TechnicalException) return e;
                    return new TechnicalException(TechnicalExceptionMessage.UNEXPECTED_ERROR);
                });
    }

    public Mono<Solicitud> actualizarEstadoConResultado(ResultadoSolicitud result) {
        return Mono.justOrEmpty(EstadoSolicitud.fromDecision(result.getDecision()))
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.STATE_NOT_FOUND)))
                .flatMap(estadoEnum ->
                        estadoRepository.findIdByNombre(estadoEnum.getNombre())
                                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.STATE_NOT_FOUND)))
                                .flatMap(idEstado ->
                                        solicitudRepository.updateEstado(result.getIdSolicitud(), idEstado)
                                                .then(solicitudRepository.findById(result.getIdSolicitud()))
                                                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.REQUEST_NOT_FOUND)))
                                )
                )
                .flatMap(solicitud ->
                        notificacionEventPublisher.send(result)
                                .then(
                                        enviarEventoAprobacionSiCorresponde(
                                                result.getDecision(),
                                                solicitud.getIdSolicitud(),
                                                solicitud.getIdUsuario(),
                                                solicitud.getMonto()
                                        ).thenReturn(solicitud)
                                )
                )
                .onErrorMap(e -> (e instanceof BusinessException || e instanceof ValidationException || e instanceof TechnicalException) ? e
                        : new TechnicalException(TechnicalExceptionMessage.UNEXPECTED_ERROR));
    }

    private Mono<Void> enviarEventoAprobacionSiCorresponde(
            String decisionOCadenaEstado,
            Long idSolicitud,
            Long idUsuario,
            Double monto
    ) {
        return Mono.justOrEmpty(EstadoSolicitud.fromDecision(decisionOCadenaEstado))
                .filter(estado -> estado == EstadoSolicitud.APROBADO)
                .flatMap(__ -> aprobacionEventPublisher.send(
                        new ReportesSolicitudesAprobadas(
                                idSolicitud,
                                idUsuario,
                                monto,
                                EstadoSolicitud.APROBADO.getNombre()
                        )
                ).then());
    }

}