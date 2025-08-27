package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitudUseCaseTest {

    private SolicitudRepository solicitudRepository;
    private TipoPrestamoRepository tipoPrestamoRepository;
    private EstadosRepository estadoRepository;
    private UsuarioGateway usuarioGateway;

    private SolicitudUseCase solicitudUseCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        tipoPrestamoRepository = Mockito.mock(TipoPrestamoRepository.class);
        estadoRepository = Mockito.mock(EstadosRepository.class);
        usuarioGateway = Mockito.mock(UsuarioGateway.class);

        solicitudUseCase = new SolicitudUseCase(
                solicitudRepository, tipoPrestamoRepository, estadoRepository, usuarioGateway);
    }

    private Solicitud buildValidSolicitud() {
        return Solicitud.builder()
                .monto(5000.0)
                .plazo(12)
                .idTipoPrestamo(1L)
                .idUsuario(1L)
                .documentoIdentidad("123456789")
                .build();
    }

    private Usuario buildUsuario() {
        return Usuario.builder()
                .idUsuario(1L)
                .documentoIdentidad("123456789")
                .build();
    }

    @Test
    void crearSolicitudSuccess() {
        Solicitud solicitud = buildValidSolicitud();
        Usuario usuario = buildUsuario();

        when(tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())).thenReturn(Mono.just(usuario));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .assertNext(saved -> {
                    assertEquals(1L, saved.getIdEstado());
                    assertEquals(1L, saved.getIdUsuario());
                })
                .verifyComplete();
    }

    @Test
    void crearSolicitudFailsWhenMontoIsNull() {
        Solicitud solicitud = buildValidSolicitud();
        solicitud.setMonto(null);

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(ValidationException.class, error);
                    assertEquals(ValidationExceptionMessage.AMOUNT_REQUIRED.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenMontoIsInvalid() {
        Solicitud solicitud = buildValidSolicitud();
        solicitud.setMonto(0.0);

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(ValidationException.class, error);
                    assertEquals(ValidationExceptionMessage.AMOUNT_INVALID.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenPlazoIsInvalid() {
        Solicitud solicitud = buildValidSolicitud();
        solicitud.setPlazo(0);

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(ValidationException.class, error);
                    assertEquals(ValidationExceptionMessage.TERM_INVALID.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenIdTipoPrestamoIsNull() {
        Solicitud solicitud = buildValidSolicitud();
        solicitud.setIdTipoPrestamo(null);

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(ValidationException.class, error);
                    assertEquals(ValidationExceptionMessage.LOAN_TYPE_REQUIRED.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenIdUsuarioIsNull() {
        Solicitud solicitud = buildValidSolicitud();
        solicitud.setIdUsuario(null);

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(BusinessException.class, error);
                    assertEquals(BusinessExceptionMessage.USER_ID_REQUIRED.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenTipoPrestamoNotFound() {
        Solicitud solicitud = buildValidSolicitud();
        Usuario usuario = buildUsuario();

        when(tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())).thenReturn(Mono.just(false));
        when(estadoRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())).thenReturn(Mono.just(usuario));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(BusinessException.class, error);
                    assertEquals(BusinessExceptionMessage.LOAN_TYPE_NOT_FOUND.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenEstadoNotFound() {
        Solicitud solicitud = buildValidSolicitud();
        Usuario usuario = buildUsuario();

        when(tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(1L)).thenReturn(Mono.just(false));
        when(usuarioGateway.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())).thenReturn(Mono.just(usuario));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(BusinessException.class, error);
                    assertEquals(BusinessExceptionMessage.STATE_NOT_FOUND.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenUsuarioNotFound() {
        Solicitud solicitud = buildValidSolicitud();

        when(tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(BusinessException.class, error);
                    assertEquals(BusinessExceptionMessage.USER_NOT_FOUND.getMessage(), error.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitudFailsWhenUsuarioServiceError() {
        Solicitud solicitud = buildValidSolicitud();

        when(tipoPrestamoRepository.existsById(solicitud.getIdTipoPrestamo())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad()))
                .thenReturn(Mono.error(new RuntimeException("timeout")));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(BusinessException.class, error);
                    assertEquals(BusinessExceptionMessage.USER_SERVICE_ERROR.getMessage(), error.getMessage());
                })
                .verify();
    }
}

