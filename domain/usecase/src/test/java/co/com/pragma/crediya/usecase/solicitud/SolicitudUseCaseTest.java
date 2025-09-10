package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.solicitud.*;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
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

    private SolicitudUseCase solicitudUseCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        tipoPrestamoRepository = Mockito.mock(TipoPrestamoRepository.class);
        estadoRepository = Mockito.mock(EstadosRepository.class);
        usuarioGateway = Mockito.mock(UsuarioGateway.class);
        notificacionEventPublisher = Mockito.mock(NotificacionEventPublisher.class);

        solicitudUseCase = new SolicitudUseCase(
                solicitudRepository, tipoPrestamoRepository, estadoRepository, usuarioGateway, notificacionEventPublisher);
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
        // Usa builder si tu clase lo tiene; de lo contrario ajusta a setters/constructor
        return SolicitudEstado.builder()
                .idSolicitud(id)
                .estado(estado)
                .build();
    }

    // METODO: crearSolicitud

    @Test
    void crearSolicitud_debeGuardarCuandoTodoEsValido() {
        Solicitud solicitud = buildSolicitudValida();
        UsuarioAutenticado auth = buildAuthUsuario();

        when(tipoPrestamoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(estadoRepository.existsById(any())).thenReturn(Mono.just(true));
        when(usuarioGateway.existsByDocumentoIdentidad(any()))
                .thenReturn(Mono.just(Usuario.builder().idUsuario(1L).build()));
        when(solicitudRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(solicitudUseCase.crearSolicitud(solicitud, auth))
                .expectNextMatches(s -> s.getIdUsuario().equals(1L) && s.getIdEstado().equals(1L))
                .verifyComplete();
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

    // =============== ERRORES NO CONTROLADOS -> UNEXPECTED_ERROR ===============

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
}