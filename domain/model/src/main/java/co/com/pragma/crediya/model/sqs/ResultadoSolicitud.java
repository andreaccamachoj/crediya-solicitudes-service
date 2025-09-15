package co.com.pragma.crediya.model.sqs;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResultadoSolicitud {
    private Long idSolicitud;
    private Long idUsuario;
    private String email;
    private String decision;
    private Double capacidadMaxima;
    private Double deudaActual;
    private Double capacidadDisponible;
    private Double cuotaNuevo;
    private List<PlanPago> planPagos;
}
