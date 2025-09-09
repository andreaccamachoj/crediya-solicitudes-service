package co.com.pragma.crediya.model.usuario;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UsuarioInfo {
    Long idUsuario;
    String nombres;
    String apellidos;
    String correoElectronico;
    String documentoIdentidad;
    Double salarioBase;
}
