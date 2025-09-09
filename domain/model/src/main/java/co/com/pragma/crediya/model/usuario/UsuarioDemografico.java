package co.com.pragma.crediya.model.usuario;

import java.sql.Timestamp;

public record UsuarioDemografico(
        Long idUsuario,
        String nombres,
        String apellidos,
        String correoElectronico,
        String documentoIdentidad,
        Timestamp fechaNacimiento,
        String telefono,
        Long idRol,
        String direccion,
        Double salarioBase
) {}

