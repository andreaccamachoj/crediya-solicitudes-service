package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.TechnicalException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.TechnicalExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.solicitud.*;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.model.sqs.CapacidadLambdaRequest;
import co.com.pragma.crediya.model.sqs.ResultadoSolicitud;
import co.com.pragma.crediya.model.sqs.gateways.AprobacionEventPublisher;
import co.com.pragma.crediya.model.sqs.gateways.CapacidadEventPublisher;
import co.com.pragma.crediya.model.sqs.gateways.NotificacionEventPublisher;
import co.com.pragma.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.UsuarioDemografico;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitudUseCaseTest {

    private SolicitudRepository solicitudRepository;
    private TipoPrestamoRepository tipoPrestamoRepository;
    private EstadosRepository estadoRepository;
    private UsuarioGateway usuarioGateway;
    private NotificacionEventPublisher notificacionEventPublisher;
    private CapacidadEventPublisher capacidadEventPublisher;
    private AprobacionEventPublisher aprobacionEventPublisher;

    private SolicitudUseCase solicitudUseCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        tipoPrestamoRepository = Mockito.mock(TipoPrestamoRepository.class);
        estadoRepository = Mockito.mock(EstadosRepository.class);
        usuarioGateway = Mockito.mock(UsuarioGateway.class);
        notificacionEventPublisher = Mockito.mock(NotificacionEventPublisher.class);
        capacidadEventPublisher = Mockito.mock(CapacidadEventPublisher.class);
        aprobacionEventPublisher = Mockito.mock(AprobacionEventPublisher.class);

        solicitudUseCase = new SolicitudUseCase(
                solicitudRepository, tipoPrestamoRepository, estadoRepository, usuarioGateway, notificacionEventPublisher, capacidadEventPublisher, aprobacionEventPublisher);
    }

    private Solicitud buildSolicitudValida() {
        return Solicitud.builder()
                .monto(20_000_000D)
                .plazo(12)
                .idTipoPrestamo(1L)
                .documentoIdentidad("123456789")
                .build();
    }

    private UsuarioAutenticado buildAuthUsuario() {
        return new UsuarioAutenticado(1L, "mariana.zapata@ejemplo.com", 2L, "CLIENTE");
    }

    private SolicitudDetalle buildSolicitudDetalle(Long idUsuario) {
        return SolicitudDetalle.builder()
                .idsolicitud(1L)
                .idusuario(idUsuario)
                .monto(20_000_000D)
                .plazo(12)
                .email("camilo.gomez@example.com")
                .tipoprestamo("Consumo")
                .tasainteres(12.5)
                .estado("PENDIENTE DE REVISIÓN")
                .build();
    }

    private UsuarioDemografico buildUsuarioDemografico(Long idUsuario) {
        return new UsuarioDemografico(idUsuario, "Camilo", "Gómez", "correo@example.com",
                "123", null, "3124567890", 2L, "Bogotá", 2_800_000D);
    }

    private UsuarioAutenticado buildAuthAsesor() {
        return new UsuarioAutenticado(10L, "asesor@empresa.com", 99L, "ASESOR");
    }

    private SolicitudEstado buildSolicitudEstado(Long id, String estado) {
        return SolicitudEstado.builder()
                .idSolicitud(id)
                .estado(estado)
                .build();
    }

    private ResultadoSolicitud buildResultadoSolicitud() {
        return new ResultadoSolicitud(
                1L,
                25L,
                "correo@ejemplo.com",
                "PENDIENTE DE REVISIÓN",
                2_800_000D,
                500_000D,
                2_300_000D,
                1_000_000D,
                List.of()
        );
    }


    @Test
    void crearSolicitud_debeFallarCuandoRolNoEsUsuario() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = new UsuarioAutenticado(1L, "mariana.zapata@ejemplo.com", 2L, "ASESOR");

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage().equals(BusinessExceptionMessage.ROL_NOT_FOUND.getMessage()))
                .verify();
    }

    @Test
    void crearSolicitud_debeFallarCuandoMontoEsNulo() {
        Solicitud solicitud = buildSolicitudValida();
        solicitud.setMonto(null);
        UsuarioAutenticado auth = buildAuthUsuario();

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof ValidationException &&
                        ex.getMessage().equals(ValidationExceptionMessage.AMOUNT_REQUIRED.getMessage()))
                .verify();
    }

    @Test
    void crearSolicitud_debeFallarCuandoUsuarioNoExiste() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any())).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage().equals(BusinessExceptionMessage.USER_NOT_FOUND.getMessage()))
                .verify();
    }

    @Test
    void crearSolicitud_debeFallarCuandoServicioUsuarioDaErrorTecnico() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any()))
                .thenReturn(Mono.error(new RuntimeException("Timeout")));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage().equals(BusinessExceptionMessage.USER_SERVICE_ERROR.getMessage()))
                .verify();
    }

    @Test
    void crearSolicitud_debeFallarCuandoIdentidadNoCoincide() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario(); // idUsuario = 1

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any()))
                .thenReturn(Mono.just(Usuario.builder().idUsuario(99L).build())); // distinto a auth

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage()
                                .equals(BusinessExceptionMessage.USER_IDENTITY_MISMATCH.getMessage()))
                .verify();
    }

    @Test
    void crearSolicitud_debeFallarCuandoEstadoNoExiste() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(false)); // no existe
        when(usuarioGateway.existsByDocumentoIdentidad(any()))
                .thenReturn(Mono.just(Usuario.builder().idUsuario(1L).build()));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage()
                                .equals(BusinessExceptionMessage.STATE_NOT_FOUND.getMessage()))
                .verify();
    }

    @Test
    void crearSolicitud_debeFallarCuandoTipoPrestamoNoExiste() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(false)); // no existe
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any()))
                .thenReturn(Mono.just(Usuario.builder().idUsuario(1L).build()));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage()
                                .equals(BusinessExceptionMessage.LOAN_TYPE_NOT_FOUND.getMessage()))
                .verify();
    }

    //METODO: listarSolicitudesPendientes
    @Test
    void listarSolicitudesPendientes_sinFiltroEmail_debeRetornarPaginaConSolicitudes() {
        // ARRANGE
        UsuarioAutenticado auth = buildAuthUsuario();
        auth.setNombreRol("ASESOR");
        SolicitudPageRequest pageRequest = new SolicitudPageRequest(0, 10);
        String email = null; // Caso de prueba sin filtro de email

        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), eq(email)))
                .thenReturn(Flux.just(25L));

        when(usuarioGateway.findAllByIds(any()))
                .thenReturn(Flux.just(buildUsuarioDemografico(25L)));

        when(solicitudRepository.findDetallesByEstadosAndUsuariosPaged(any(), any(), any()))
                .thenReturn(Mono.just(new SolicitudPageResponse<>(List.of(buildSolicitudDetalle(25L)), 1L, 0, 10)));

        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any()))
                .thenReturn(Flux.empty()); // sin aprobadas

        // ACT & ASSERT
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(pageRequest, auth, email))
                .assertNext(resp -> {
                    assertEquals(1, resp.getTotalElements());
                    assertEquals("Camilo Gómez", resp.getContent().get(0).getNombre());
                    assertEquals(0.0, resp.getContent().get(0).getDeudaTotalMensualAprobadas());
                })
                .verifyComplete();
    }

    @Test
    void listarSolicitudesPendientes_debeFallarCuandoRolNoEsAsesor() {
        // ARRANGE
        UsuarioAutenticado auth = buildAuthUsuario();
        String email = null;

        // ACT & ASSERT
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(new SolicitudPageRequest(0, 10), auth, email))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ex.getMessage()
                                .equals(BusinessExceptionMessage.ROL_NOT_ASESOR.getMessage()))
                .verify();
    }

    @Test
    void listarSolicitudesPendientes_sinFiltroEmail_debeRetornarVacioCuandoNoHayUsuarios() {
        // ARRANGE
        UsuarioAutenticado auth = buildAuthUsuario();
        auth.setNombreRol("ASESOR");
        String email = null;

        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), eq(email)))
                .thenReturn(Flux.empty());

        // ACT & ASSERT
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(new SolicitudPageRequest(0, 10), auth, email))
                .assertNext(resp -> {
                    assertEquals(0, resp.getTotalElements());
                    assertTrue(resp.getContent().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void listarSolicitudesPendientes_usuarioConSolicitudesAprobadasDebeCalcularDeuda() {
        // ARRANGE
        UsuarioAutenticado auth = buildAuthUsuario();
        auth.setNombreRol("ASESOR");
        SolicitudPageRequest pageRequest = new SolicitudPageRequest(0, 10);
        String email = null;

        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), eq(email)))
                .thenReturn(Flux.just(25L));

        when(usuarioGateway.findAllByIds(any()))
                .thenReturn(Flux.just(buildUsuarioDemografico(25L)));

        when(solicitudRepository.findDetallesByEstadosAndUsuariosPaged(any(), any(), any()))
                .thenReturn(Mono.just(new SolicitudPageResponse<>(List.of(buildSolicitudDetalle(25L)), 1L, 0, 10)));

        SolicitudDetalle aprobada = SolicitudDetalle.builder()
                .idusuario(25L)
                .monto(12_000_000D)
                .plazo(12)
                .tasainteres(12.0)
                .build();

        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any()))
                .thenReturn(Flux.just(aprobada));

        // ACT & ASSERT
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(pageRequest, auth, email))
                .assertNext(resp -> {
                    Double deuda = resp.getContent().get(0).getDeudaTotalMensualAprobadas();
                    assertTrue(deuda > 0.0, "La deuda debería ser mayor que cero si hay solicitudes aprobadas");
                })
                .verifyComplete();
    }

    @Test
    void listarSolicitudesPendientes_debeFallarCuandoServicioUsuariosDaError() {
        // ARRANGE
        UsuarioAutenticado auth = buildAuthUsuario();
        auth.setNombreRol("ASESOR");
        SolicitudPageRequest pageRequest = new SolicitudPageRequest(0, 10);
        String email = null;

        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), eq(email)))
                .thenReturn(Flux.just(25L));

        when(usuarioGateway.findAllByIds(any()))
                .thenReturn(Flux.error(new RuntimeException("Auth service unavailable")));

        when(solicitudRepository.findDetallesByEstadosAndUsuariosPaged(any(), any(), any()))
                .thenReturn(Mono.empty());

        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(pageRequest, auth, email))
                .expectErrorMatches(ex -> ex instanceof RuntimeException &&
                        ex.getMessage().equals("Auth service unavailable"))
                .verify();
    }
    @Test
    void listarSolicitudesPendientes_conFiltroEmail_debeInvocarRepositorioConEmailCorrecto() {
        // ARRANGE
        UsuarioAutenticado auth = buildAuthUsuario();
        auth.setNombreRol("ASESOR");
        SolicitudPageRequest pageRequest = new SolicitudPageRequest(0, 10);
        String emailFiltro = "usuario.filtrado@crediva.com";

        // Mock para que el repositorio responda cuando se le llame CON el email correcto
        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), eq(emailFiltro)))
                .thenReturn(Flux.just(26L));

        when(usuarioGateway.findAllByIds(any()))
                .thenReturn(Flux.just(buildUsuarioDemografico(26L)));

        when(solicitudRepository.findDetallesByEstadosAndUsuariosPaged(any(), any(), any()))
                .thenReturn(Mono.just(new SolicitudPageResponse<>(List.of(buildSolicitudDetalle(26L)), 1L, 0, 10)));

        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any()))
                .thenReturn(Flux.empty());

        // ACT & ASSERT
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(pageRequest, auth, emailFiltro))
                .assertNext(resp -> {
                    assertEquals(1, resp.getTotalElements());
                    assertFalse(resp.getContent().isEmpty());
                })
                .verifyComplete();

        verify(solicitudRepository, times(1)).findDistinctIdUsuariosByEstados(any(), eq(emailFiltro));
    }


    // METODO: actualizarEstado

    @Test
    void actualizarEstadoSolicitud_exito() {
        // Arrange
        var req = buildSolicitudEstado(1L, "  aprobado  "); // probar trim + upper
        var auth = buildAuthAsesor();
        var detalleActualizado = buildSolicitudDetalle(123L);

        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(buildSolicitudValida()));
        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(7L));
        when(solicitudRepository.updateEstado(1L, 7L)).thenReturn(Mono.empty());
        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(detalleActualizado));
        when(notificacionEventPublisher.send(detalleActualizado)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectNext(detalleActualizado)
                .verifyComplete();

        verify(solicitudRepository).findById(1L);
        verify(estadoRepository).findIdByNombre("APROBADO");
        verify(solicitudRepository).updateEstado(1L, 7L);
        verify(solicitudRepository).findDetallesByIdSolicitud(1L);
        verify(notificacionEventPublisher).send(detalleActualizado);
    }

    @Test
    void actualizarEstadoSolicitud_enviaEstadoUpperTrim() {
        // Arrange
        var req = buildSolicitudEstado(1L, "  pendiente de revisión "); // debería llamar con "PENDIENTE DE REVISIÓN"
        var auth = buildAuthAsesor();

        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(buildSolicitudValida()));
        when(estadoRepository.findIdByNombre(anyString())).thenReturn(Mono.just(2L));
        when(solicitudRepository.updateEstado(1L, 2L)).thenReturn(Mono.empty());
        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(buildSolicitudDetalle(5L)));
        when(notificacionEventPublisher.send(any())).thenReturn(Mono.empty());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // Act
        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectNextCount(1)
                .verifyComplete();

        // Assert
        verify(estadoRepository).findIdByNombre(captor.capture());
        assertEquals("PENDIENTE DE REVISIÓN", captor.getValue());
    }

    // =============== VALIDACIONES (rol/inputs) ===============

    @Test
    void actualizarEstadoSolicitud_fallaPorRolNoAsesor() {
        var req = buildSolicitudEstado(1L, "APROBADO");
        var authNoAsesor = buildAuthUsuario(); // tu helper devuelve rol CLIENTE

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, authNoAsesor))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.ROL_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();

        verifyNoInteractions(solicitudRepository, estadoRepository, notificacionEventPublisher);
    }

    @Test
    void actualizarEstadoSolicitud_fallaPorIdSolicitudNull() {
        var req = buildSolicitudEstado(null, "APROBADO");
        var auth = buildAuthAsesor();

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(ValidationException.class, err);
                    assertEquals(ValidationExceptionMessage.STATE_REQUIRED.getMessage(), err.getMessage());
                })
                .verify();

        verifyNoInteractions(solicitudRepository, estadoRepository, notificacionEventPublisher);
    }

    @Test
    void actualizarEstadoSolicitud_fallaPorEstadoNull() {
        var req = buildSolicitudEstado(1L, null);
        var auth = buildAuthAsesor();

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(ValidationException.class, err);
                    assertEquals(ValidationExceptionMessage.STATE_REQUIRED.getMessage(), err.getMessage());
                })
                .verify();

        verifyNoInteractions(solicitudRepository, estadoRepository, notificacionEventPublisher);
    }

    // =============== NO ENCONTRADOS ===============

    @Test
    void actualizarEstadoSolicitud_fallaSolicitudNoExiste() {
        var req = buildSolicitudEstado(1L, "APROBADO");
        var auth = buildAuthAsesor();

        when(solicitudRepository.findById(1L)).thenReturn(Mono.empty());
        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.never());

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.REQUEST_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();

        verify(solicitudRepository).findById(1L);
        verify(estadoRepository).findIdByNombre("APROBADO");
        verifyNoInteractions(notificacionEventPublisher);
    }


    @Test
    void actualizarEstadoSolicitud_fallaEstadoDestinoNoExiste() {
        var req = buildSolicitudEstado(1L, "APROBADO");
        var auth = buildAuthAsesor();

        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(buildSolicitudValida()));
        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.STATE_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();

        verify(solicitudRepository).findById(1L);
        verify(estadoRepository).findIdByNombre("APROBADO");
        verifyNoMoreInteractions(solicitudRepository, estadoRepository);
        verifyNoInteractions(notificacionEventPublisher);
    }

    @Test
    void actualizarEstadoSolicitud_fallaSiNoHayDetalleLuegoDeActualizar() {
        var req = buildSolicitudEstado(1L, "APROBADO");
        var auth = buildAuthAsesor();

        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(buildSolicitudValida()));
        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(5L));
        when(solicitudRepository.updateEstado(1L, 5L)).thenReturn(Mono.empty());
        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.REQUEST_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();

        verify(notificacionEventPublisher, never()).send(any());
    }

    @Test
    void actualizarEstadoSolicitud_envuelveErroresEnUnexpected() {
        var req = buildSolicitudEstado(1L, "APROBADO");
        var auth = buildAuthAsesor();

        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(buildSolicitudValida()));
        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(9L));
        when(solicitudRepository.updateEstado(1L, 9L)).thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.UNEXPECTED_ERROR.getMessage(), err.getMessage());
                })
                .verify();
    }

    @Test
    void actualizarEstadoSolicitud_unexpectedErrorEnPublisher() {
        var req = buildSolicitudEstado(1L, "APROBADO");
        var auth = buildAuthAsesor();
        var detalle = buildSolicitudDetalle(222L);

        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(buildSolicitudValida()));
        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(3L));
        when(solicitudRepository.updateEstado(1L, 3L)).thenReturn(Mono.empty());
        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(detalle));
        when(notificacionEventPublisher.send(detalle)).thenReturn(Mono.error(new RuntimeException("SNS/SES down")));

        StepVerifier.create(solicitudUseCase.actualizarEstadoSolicitud(req, auth))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.UNEXPECTED_ERROR.getMessage(), err.getMessage());
                })
                .verify();

    }

    @Test
    void prepararMensajeCapacidadParaLambda_exito() {
        SolicitudDetalle detalle = buildSolicitudDetalle(1L);
        UsuarioDemografico usuario = buildUsuarioDemografico(1L);

        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(detalle));
        when(usuarioGateway.findAllByIds(List.of(1L))).thenReturn(Flux.just(usuario));
        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(List.of(1L)))
                .thenReturn(Flux.just(detalle));
        when(capacidadEventPublisher.send(any(CapacidadLambdaRequest.class)))
                .thenReturn(Mono.just("OK"));

        StepVerifier.create(solicitudUseCase.prepararMensajeCapacidadParaLambda(1L))
                .expectNext("OK")
                .verifyComplete();

        verify(capacidadEventPublisher).send(any(CapacidadLambdaRequest.class));
    }

    @Test
    void prepararMensajeCapacidadParaLambda_solicitudNoExiste() {
        when(solicitudRepository.findDetallesByIdSolicitud(99L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.prepararMensajeCapacidadParaLambda(99L))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.REQUEST_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();
    }

    @Test
    void prepararMensajeCapacidadParaLambda_usuarioNoExiste() {
        SolicitudDetalle detalle = buildSolicitudDetalle(1L);

        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(detalle));
        when(usuarioGateway.findAllByIds(List.of(1L))).thenReturn(Flux.empty());

        StepVerifier.create(solicitudUseCase.prepararMensajeCapacidadParaLambda(1L))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(TechnicalException.class, err);
                    assertEquals(
                            TechnicalExceptionMessage.UNEXPECTED_ERROR.getMessage(),
                            err.getMessage()
                    );
                })
                .verify();
    }


    @Test
    void prepararMensajeCapacidadParaLambda_usuarioServiceError() {
        SolicitudDetalle detalle = buildSolicitudDetalle(1L);

        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(detalle));
        when(usuarioGateway.findAllByIds(List.of(1L))).thenReturn(Flux.error(new RuntimeException("fallo")));

        StepVerifier.create(solicitudUseCase.prepararMensajeCapacidadParaLambda(1L))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(TechnicalException.class, err);
                    assertEquals(
                            TechnicalExceptionMessage.UNEXPECTED_ERROR.getMessage(),
                            err.getMessage()
                    );
                })
                .verify();
    }


//    @Test
//    void actualizarEstadoConResultado_exito() {
//        ResultadoSolicitud resultado = buildResultadoSolicitud();
//        resultado.setDecision("APROBADO");
//
//        Solicitud solicitud = buildSolicitudValida();
//
//        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(2L));
//        when(solicitudRepository.updateEstado(1L, 2L)).thenReturn(Mono.empty());
//        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(solicitud));
//        when(notificacionEventPublisher.send(resultado)).thenReturn(Mono.just("OK"));
//
//        StepVerifier.create(solicitudUseCase.actualizarEstadoConResultado(resultado))
//                .expectNext(solicitud)
//                .verifyComplete();
//
//        verify(solicitudRepository).updateEstado(1L, 2L);
//        verify(notificacionEventPublisher).send(resultado);
//    }


    @Test
    void actualizarEstadoConResultado_estadoInvalido() {
        ResultadoSolicitud resultado = buildResultadoSolicitud();
        resultado.setDecision("INVALIDO");

        StepVerifier.create(solicitudUseCase.actualizarEstadoConResultado(resultado))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.STATE_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();
    }

    @Test
    void actualizarEstadoConResultado_estadoNoExisteEnDb() {
        ResultadoSolicitud resultado = buildResultadoSolicitud();
        resultado.setDecision("APROBADO");

        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.actualizarEstadoConResultado(resultado))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.STATE_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();
    }

    @Test
    void actualizarEstadoConResultado_solicitudNoExiste() {
        ResultadoSolicitud resultado = buildResultadoSolicitud();
        resultado.setDecision("APROBADO");

        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(2L));
        when(solicitudRepository.updateEstado(1L, 2L)).thenReturn(Mono.empty());
        when(solicitudRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.actualizarEstadoConResultado(resultado))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(BusinessExceptionMessage.REQUEST_NOT_FOUND.getMessage(), err.getMessage());
                })
                .verify();
    }

    @Test
    void crearSolicitud_exitoConValidacionAutomatica() {
        // Arrange
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        SolicitudDetalle detalle = buildSolicitudDetalle(1L);
        UsuarioDemografico usuarioDemo = buildUsuarioDemografico(1L);

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any())).thenReturn(Mono.just(usuario));
        when(solicitudRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(tipoPrestamoRepository.isValidacionAutomaticaEnabled(any())).thenReturn(Mono.just(true));

        // mocks adicionales para prepararMensajeCapacidadParaLambda
        when(solicitudRepository.findDetallesByIdSolicitud(any())).thenReturn(Mono.just(detalle));
        when(usuarioGateway.findAllByIds(any())).thenReturn(Flux.just(usuarioDemo));
        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any())).thenReturn(Flux.empty());

        when(capacidadEventPublisher.send(any())).thenReturn(Mono.just("OK"));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectNextMatches(s -> s.getIdUsuario().equals(1L) && s.getIdEstado().equals(1L))
                .verifyComplete();

        verify(capacidadEventPublisher).send(any());
    }


    @Test
    void listarSolicitudesPendientes_multiplesUsuarios() {
        // Arrange
        UsuarioAutenticado auth = buildAuthAsesor();
        SolicitudPageRequest pageRequest = new SolicitudPageRequest(0, 10);

        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), any()))
                .thenReturn(Flux.just(25L, 26L));

        when(usuarioGateway.findAllByIds(any()))
                .thenReturn(Flux.just(buildUsuarioDemografico(25L), buildUsuarioDemografico(26L)));

        when(solicitudRepository.findDetallesByEstadosAndUsuariosPaged(any(), any(), any()))
                .thenReturn(Mono.just(new SolicitudPageResponse<>(
                        List.of(buildSolicitudDetalle(25L), buildSolicitudDetalle(26L)),
                        2L, 0, 10)));

        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any()))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(pageRequest, auth, null))
                .assertNext(resp -> {
                    assertEquals(2, resp.getTotalElements());
                    assertEquals(2, resp.getContent().size());
                })
                .verifyComplete();
    }

    @Test
    void calcularCuotaMensual_sinTasa() {
        // Arrange
        UsuarioAutenticado auth = buildAuthAsesor(); // rol ASESOR
        SolicitudPageRequest pageRequest = new SolicitudPageRequest(0, 10);

        // 1) El repositorio debe devolver un usuario candidato
        when(solicitudRepository.findDistinctIdUsuariosByEstados(any(), any()))
                .thenReturn(Flux.just(25L));

        // 2) Datos demográficos del usuario
        when(usuarioGateway.findAllByIds(any()))
                .thenReturn(Flux.just(buildUsuarioDemografico(25L)));

        // 3) Página con 1 solicitud en estado pendiente (el listado que se retorna)
        when(solicitudRepository.findDetallesByEstadosAndUsuariosPaged(any(), any(), any()))
                .thenReturn(Mono.just(new SolicitudPageResponse<>(
                        List.of(buildSolicitudDetalle(25L)), // usa tu helper con monto/plazo válidos
                        1L, 0, 10)));

        // 4) Préstamo APROBADO existente (el que usará la fórmula sin tasa)
        SolicitudDetalle aprobadaSinTasa = SolicitudDetalle.builder()
                .idusuario(25L)          // importante: debe coincidir con el usuario listado
                .monto(12_000_000D)      // monto
                .plazo(12)               // plazo > 0
                .tasainteres(0.0)        // tasa = 0.0 → cuota = monto/plazo
                .build();

        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(any()))
                .thenReturn(Flux.just(aprobadaSinTasa));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.listarSolicitudesPendientes(pageRequest, auth, null))
                .assertNext(resp -> {
                    assertEquals(1, resp.getTotalElements());
                    Double deuda = resp.getContent().get(0).getDeudaTotalMensualAprobadas();
                    // 12_000_000 / 12 = 1_000_000
                    assertEquals(1_000_000D, deuda, 0.0001, "La cuota debe ser monto/plazo cuando la tasa es 0");
                })
                .verifyComplete();
    }

    @Test
    void crearSolicitud_exitoSinValidacionAutomatica() {
        // Arrange
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any()))
                .thenReturn(Mono.just(Usuario.builder().idUsuario(1L).build()));
        when(solicitudRepository.save(any()))
                .thenAnswer(inv -> Mono.just(((Solicitud) inv.getArgument(0)).toBuilder().idSolicitud(20L).build()));
        when(tipoPrestamoRepository.isValidacionAutomaticaEnabled(any()))
                .thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectNextMatches(s -> s.getIdUsuario().equals(1L)
                        && s.getIdEstado().equals(1L)
                        && s.getIdSolicitud().equals(20L))
                .verifyComplete();

        // Verify interactions
        verify(solicitudRepository).save(any(Solicitud.class));
        verify(capacidadEventPublisher, never()).send(any());
    }

    @Test
    void prepararMensajeCapacidadParaLambda_fallaPublisher() {
        // Arrange
        SolicitudDetalle detalle = buildSolicitudDetalle(1L);
        UsuarioDemografico usuario = buildUsuarioDemografico(1L);

        when(solicitudRepository.findDetallesByIdSolicitud(1L)).thenReturn(Mono.just(detalle));
        when(usuarioGateway.findAllByIds(List.of(1L))).thenReturn(Flux.just(usuario));
        when(solicitudRepository.findSolicitudesAprobadasByUsuarios(List.of(1L)))
                .thenReturn(Flux.just(detalle));
        when(capacidadEventPublisher.send(any(CapacidadLambdaRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("SQS down")));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.prepararMensajeCapacidadParaLambda(1L))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(TechnicalException.class, err);
                    assertEquals(
                            TechnicalExceptionMessage.UNEXPECTED_ERROR.getMessage(),
                            err.getMessage()
                    );
                })
                .verify();

        verify(capacidadEventPublisher).send(any(CapacidadLambdaRequest.class));
    }

    @Test
    void actualizarEstadoConResultado_fallaPublisher() {
        ResultadoSolicitud resultado = buildResultadoSolicitud();
        resultado.setDecision("APROBADO");
        Solicitud solicitud = buildSolicitudValida();

        when(estadoRepository.findIdByNombre("APROBADO")).thenReturn(Mono.just(2L));
        when(solicitudRepository.updateEstado(1L, 2L)).thenReturn(Mono.empty());
        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(solicitud));
        when(notificacionEventPublisher.send(resultado))
                .thenReturn(Mono.error(new RuntimeException("SNS/SES down")));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.actualizarEstadoConResultado(resultado))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(TechnicalException.class, err);
                    assertEquals(
                            TechnicalExceptionMessage.UNEXPECTED_ERROR.getMessage(),
                            err.getMessage()
                    );
                })
                .verify();

        verify(notificacionEventPublisher).send(resultado);
    }


}