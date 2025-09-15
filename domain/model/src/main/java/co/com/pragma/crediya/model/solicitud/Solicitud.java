package co.com.pragma.crediya.model.solicitud;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {
    private Double monto;
    private Long idSolicitud;
    private String documentoIdentidad;
    private Integer plazo;
    private String email;
    private Long idTipoPrestamo;
    private Long idEstado;
    private Long idUsuario;
}
