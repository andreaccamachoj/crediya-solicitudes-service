package co.com.pragma.crediya.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SolicitudCambioEstadoResponse {
    private Long idSolicitud;
    private String estado;
    private String email;
    private Long idUsuario;
    private Double monto;
    private Integer plazo;
    private String tipoPrestamo;
    private Double tasaInteres;
    private String mensaje;
}
