package co.com.pragma.crediya.api;

import co.com.pragma.crediya.api.config.SolicitudPath;
import co.com.pragma.crediya.api.dto.request.CrearSolicitudRequest;
import co.com.pragma.crediya.api.dto.response.SolicitudResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    private final SolicitudPath solicitudPath;
    private final Handler solicitudHandler;

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
                                                    ),
                                                    @ExampleObject(
                                                            name = "snake_case_alias",
                                                            value = """
                                {
                                  "monto": 800000,
                                  "plazo": 12,
                                  "id_tipo_prestamo": 1,
                                  "documentoIdentidad": "55667788",
                                  "email": "carlos@example.com"
                                }"""
                                                    )
                                            }
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Solicitud creada",
                                            content = @Content(
                                                    schema = @Schema(implementation = SolicitudResponse.class)
                                            )
                                    )}
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(solicitudPath.getSolicitud()), solicitudHandler::listenSaveSolicitud);
    }
}
