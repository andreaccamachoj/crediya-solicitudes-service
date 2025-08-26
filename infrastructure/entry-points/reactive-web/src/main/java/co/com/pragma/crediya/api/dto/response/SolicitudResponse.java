package co.com.pragma.crediya.api.dto.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudResponse {
    private Double monto;
    private Integer plazo;
    private String email;
    private Long idTipoPrestamo;
    private Long idEstado;
    private Long idUsuario;
}
