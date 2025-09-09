package co.com.pragma.crediya.model.solicitud;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SolicitudRevisionResponse {
    Double monto;
    Integer plazo;
    String email;
    String nombre;
    Long idUsuario;
    String tipoPrestamo;
    Double tasaInteres;
    String estadoSolicitud;
    Double salarioBase;
    Double deudaTotalMensualAprobadas;
}
