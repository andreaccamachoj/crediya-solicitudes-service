package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadosRepository estadoRepository;

    private final UsuarioGateway usuarioGateway;

    private static final Long idEstadoPendienteRevision = 1L;

    public Mono<Solicitud> crearSolicitud(Solicitud solicitud) {
        if (solicitud.getMonto() == null)
            return Mono.error(new ValidationException(ValidationExceptionMessage.AMOUNT_REQUIRED));
        if (solicitud.getMonto() <= 0)
            return Mono.error(new ValidationException(ValidationExceptionMessage.AMOUNT_INVALID));
        if (solicitud.getPlazo() == null || solicitud.getPlazo() <= 0)
            return Mono.error(new ValidationException(ValidationExceptionMessage.TERM_INVALID));
        if (solicitud.getIdTipoPrestamo() == null)
            return Mono.error(new ValidationException(ValidationExceptionMessage.LOAN_TYPE_REQUIRED));

        Mono<Void> validaTipo = tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.LOAN_TYPE_NOT_FOUND)))
                .then();

        Mono<Void> validaEstado = estadoRepository.existsById(idEstadoPendienteRevision)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.STATE_NOT_FOUND)))
                .then();

        Mono<Usuario> usuarioMono = usuarioGateway
                .existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())
                .switchIfEmpty(Mono.error(new BusinessException(BusinessExceptionMessage.USER_NOT_FOUND)))
                .flatMap(usuario -> {
                    Long idUsuario = usuario.getIdUsuario();
                    if (idUsuario == null || idUsuario <= 0) {
                        return Mono.error(new BusinessException(BusinessExceptionMessage.USER_ID_REQUIRED));
                    }
                    return Mono.just(usuario);
                })
                .onErrorResume(ex -> Mono.error(new BusinessException(BusinessExceptionMessage.USER_SERVICE_ERROR)));

        return Mono.when(validaTipo, validaEstado)
                .then(usuarioMono)
                .flatMap(usuario -> {
                    Solicitud toSave = solicitud.toBuilder()
                            .idEstado(idEstadoPendienteRevision)
                            .idUsuario(usuario.getIdUsuario())
                            .build();
                    return solicitudRepository.save(toSave);
                });
    }

}
