package co.com.pragma.crediya.model.estados;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Estados {

    private Long idEstado;
    private String nombre;
    private String descripcion;
}
