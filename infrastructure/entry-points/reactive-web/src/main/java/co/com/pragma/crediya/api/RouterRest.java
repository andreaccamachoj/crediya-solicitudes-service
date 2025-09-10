package co.com.pragma.crediya.api;

import co.com.pragma.crediya.api.config.SolicitudPath;
import co.com.pragma.crediya.api.dto.request.CrearSolicitudRequest;
import co.com.pragma.crediya.api.dto.response.SolicitudResponse;
import co.com.pragma.crediya.model.solicitud.SolicitudPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    private final SolicitudPath solicitudPath;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "listenSaveSolicitud",
                    operation = @Operation(
                            operationId = "crearSolicitud",
                            summary = "Crear solicitud de crédito",
                            description = "Permite a un usuario autenticado crear una nueva solicitud de crédito.",
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            schema = @Schema(implementation = CrearSolicitudRequest.class),
                                            examples = {
                                                    @ExampleObject(
                                                            name = "camelCase",
                                                            value = """
                                    {
                                      "monto": 1500000,
                                      "plazo": 24,
                                      "idTipoPrestamo": 2,
                                      "documentoIdentidad": "1020304050",
                                      "email": "ana@example.com"
                                    }"""
                                                    )
                                            }
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Solicitud creada exitosamente.",
                                            content = @Content(
                                                    schema = @Schema(implementation = SolicitudResponse.class)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Error de validación en los datos de entrada."
                                    ),
                                    @ApiResponse(
                                            responseCode = "401",
                                            description = "No autorizado. Se requiere autenticación."
                                    )
                            },
                            security = { @SecurityRequirement(name = "bearerAuth") }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud/listar",
                    produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.GET,
                    beanClass = Handler.class,
                    beanMethod = "listarSolicitudes",
                    operation = @Operation(
                            operationId = "listarSolicitudes",
                            summary = "Listar solicitudes de crédito",
                            description = "Obtiene una lista paginada de las solicitudes de crédito del usuario autenticado.",
                            parameters = {
                                    @Parameter(
                                            name = "page",
                                            in = ParameterIn.QUERY,
                                            description = "Número de página (0 por defecto).",
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "0")
                                    ),
                                    @Parameter(
                                            name = "size",
                                            in = ParameterIn.QUERY,
                                            description = "Cantidad de registros por página (10 por defecto).",
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "10")
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Listado de solicitudes obtenido exitosamente.",
                                            content = @Content(
                                                    schema = @Schema(implementation = SolicitudPageResponse.class)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "401",
                                            description = "No autorizado. Se requiere autenticación."
                                    ),
                                    @ApiResponse(
                                            responseCode = "500",
                                            description = "Error interno al listar las solicitudes."
                                    )
                            },
                            security = { @SecurityRequirement(name = "bearerAuth") }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud/update",
                    produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.PUT,
                    beanClass = Handler.class,
                    beanMethod = "actualizarEstado",
                    operation = @Operation(
                            operationId = "actualizarEstadoSolicitud",
                            summary = "Actualizar estado de una solicitud",
                            description = """
                        Permite a un usuario con rol **ASESOR** cambiar el estado de una solicitud a
                        **APROBADO** o **RECHAZADO**. 
                        En caso exitoso, retorna los datos del solicitante (usuario) asociados a la solicitud.
                        """,
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            schema = @Schema(
                                                    implementation = co.com.pragma.crediya.api.dto.request.SolicitudCambioEstadoRequest.class,
                                                    description = "Cuerpo para cambiar el estado. `estado` debe ser APROBADO o RECHAZADO."
                                            ),
                                            examples = {
                                                    @ExampleObject(
                                                            name = "Aprobar",
                                                            value = """
                                                {
                                                  "idSolicitud": 31,
                                                  "estado": "APROBADO"
                                                }"""
                                                    )
                                            }
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Estado actualizado correctamente. Se retorna el solicitante.",
                                            content = @Content(
                                                    schema = @Schema(implementation = co.com.pragma.crediya.model.usuario.UsuarioDemografico.class),
                                                    examples = @ExampleObject(
                                                            name = "UsuarioSolicitante",
                                                            value = """
                                                {
                                                  "idUsuario": 25,
                                                  "nombres": "Camilo",
                                                  "apellidos": "Gómez",
                                                  "correoElectronico": "camilo.gomez@example.com",
                                                  "documentoIdentidad": "1023456789",
                                                  "fechaNacimiento": "1990-05-11T19:00:00.000-05:00",
                                                  "telefono": "+57 3124567890",
                                                  "idRol": 3,
                                                  "direccion": "Carrera 10 #25-30, Bogotá",
                                                  "salarioBase": 2800000.0
                                                }"""
                                                    )
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Error de negocio o validación (por ejemplo, rol inválido, estado inválido, solicitud no encontrada)."),
                                    @ApiResponse(responseCode = "401", description = "No autorizado. Se requiere autenticación con Bearer token."),
                                    @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
                            },
                            security = { @SecurityRequirement(name = "bearerAuth") }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(solicitudPath.getSolicitud()), handler::listenSaveSolicitud)
                .andRoute(GET(solicitudPath.getListSolicitud()), handler::listarSolicitudes)
                .andRoute(PUT(solicitudPath.getUpdateSolicitud()), handler::actualizarEstado);
    }

}