package co.com.pragma.crediya.model.usuario;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Usuario {
    private Long idUsuario;
    private String documentoIdentidad;
    private String correoElectronico;
}
