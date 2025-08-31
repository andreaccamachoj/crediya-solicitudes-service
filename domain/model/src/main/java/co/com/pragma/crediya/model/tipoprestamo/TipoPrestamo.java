package co.com.pragma.crediya.model.tipoprestamo;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TipoPrestamo {

    private Long idTipoPrestamo;
    private String nombre;
    private Double montoMinimo;
    private Double montoMaximo;
    private Double tasaInteres;
    private Boolean validacionAutomatica;
}
