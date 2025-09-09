package co.com.pragma.crediya.usecase.solicitud.mapper;

import co.com.pragma.crediya.model.usuario.UsuarioDemografico;
import co.com.pragma.crediya.model.solicitud.SolicitudDetalle;
import co.com.pragma.crediya.model.solicitud.SolicitudPageResponse;
import co.com.pragma.crediya.model.solicitud.SolicitudRevisionResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SolicitudRevisionResponseMapper {

    public static SolicitudPageResponse<SolicitudRevisionResponse> buildResponse(
            Map<Long, UsuarioDemografico> usuarios,
            SolicitudPageResponse<SolicitudDetalle> pageSolicitudes,
            Map<Long, Double> deudas
    ) {
        List<SolicitudRevisionResponse> responses = pageSolicitudes.getContent().stream()
                .map(s -> {
                    UsuarioDemografico ud = usuarios.get(s.getIdusuario());
                    Double deuda = deudas.getOrDefault(s.getIdusuario(), 0.0);

                    return SolicitudRevisionResponse.builder()
                            .monto(s.getMonto())
                            .plazo(s.getPlazo())
                            .email(s.getEmail())
                            .nombre(ud != null ? ud.nombres() + " " + ud.apellidos() : null)
                            .tipoPrestamo(s.getTipoprestamo())
                            .tasaInteres(s.getTasainteres())
                            .estadoSolicitud(s.getEstado())
                            .salarioBase(ud != null && ud.salarioBase() != null ? ud.salarioBase().doubleValue() : null)
                            .idUsuario(s.getIdusuario())
                            .deudaTotalMensualAprobadas(deuda)
                            .build();
                })
                .collect(Collectors.toList());

        return SolicitudPageResponse.<SolicitudRevisionResponse>builder()
                .content(responses)
                .totalElements(pageSolicitudes.getTotalElements())
                .page(pageSolicitudes.getPage())
                .size(pageSolicitudes.getSize())
                .build();
    }
}