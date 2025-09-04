package co.com.pragma.crediya.model.solicitud;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SolicitudDetalle {
    Long idsolicitud;
    Double monto;
    Integer plazo;
    String email;
    Long idusuario;
    String tipoprestamo;
    Double tasainteres;
    String estado;
}
