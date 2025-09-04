package co.com.pragma.crediya.api.dto.response;

import lombok.Builder;

@Builder
public record SolicitudPendienteResponse(
        Double monto,
        Integer plazo,
        String email,
        String nombreUsuario,
        String tipoPrestamo,
        Double tasaInteres,
        String estadoSolicitud,
        Double salarioBase,
        Double deudaTotalMensualAprobadas
) {}
