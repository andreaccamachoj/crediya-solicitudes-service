package co.com.pragma.crediya.consumer.dto.response;

import lombok.Builder;

@Builder
public record UsuariosSolicitudResponse(
        Long idUsuario,
        String nombres,
        String apellidos,
        String correoElectronico,
        String documentoIdentidad,
        String fechaNacimiento,
        String telefono,
        Long idRol,
        String direccion,
        Double salarioBase
) {}
