package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import co.com.pragma.crediya.utils.ValidationHelper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadosRepository estadoRepository;
    private final UsuarioGateway usuarioGateway;

    private static final Long ID_ESTADO_PENDIENTE_REVISION = 1L;

    public Mono<Solicitud> crearSolicitud(Solicitud solicitud) {
        return Mono.just(solicitud)
                .flatMap(this::validarCamposRequeridos)
                .flatMap(this::procesarValidacionesAsincronasYGuardar);
    }

    private Mono<Solicitud> validarCamposRequeridos(Solicitud s) {
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


    private Mono<Solicitud> procesarValidacionesAsincronasYGuardar(Solicitud solicitud) {

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
                    Solicitud solicitudParaGuardar = solicitud.toBuilder()
                            .idEstado(ID_ESTADO_PENDIENTE_REVISION)
                            .idUsuario(usuario.getIdUsuario())
                            .build();

                    return solicitudRepository.save(solicitudParaGuardar);
                });
    }
}