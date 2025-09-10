package co.com.pragma.crediya.api.dto.request;

public record SolicitudCambioEstadoRequest(
        Long idSolicitud,
        String estado     // opcional
) {}
