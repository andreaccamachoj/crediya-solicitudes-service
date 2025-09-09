package co.com.pragma.crediya.usecase.solicitud;

import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.exception.BusinessException;
import co.com.pragma.crediya.model.exception.ValidationException;
import co.com.pragma.crediya.model.exception.message.BusinessExceptionMessage;
import co.com.pragma.crediya.model.exception.message.ValidationExceptionMessage;
import co.com.pragma.crediya.model.solicitud.Solicitud;
import co.com.pragma.crediya.model.solicitud.SolicitudDetalle;
import co.com.pragma.crediya.model.solicitud.SolicitudPageRequest;
import co.com.pragma.crediya.model.solicitud.SolicitudPageResponse;
import co.com.pragma.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.UsuarioDemografico;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}