package co.com.pragma.crediya.model.autenticacion;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAutenticado {
    private Long idUsuario;
    private String correoElectronico;
    private Long idRol;
    private String nombreRol;
}
