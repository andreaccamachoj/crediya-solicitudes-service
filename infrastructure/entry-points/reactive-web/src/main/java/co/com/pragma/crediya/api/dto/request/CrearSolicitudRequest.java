package co.com.pragma.crediya.api.dto.request;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearSolicitudRequest {
    private Double monto;
    private Integer plazo;
    @JsonAlias({"id_tipo_prestamo"})
    private Long idTipoPrestamo;
    private String documentoIdentidad;
    private String email;
}
