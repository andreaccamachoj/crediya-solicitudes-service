package co.com.pragma.crediya.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RolNombre {

    ADMINISTRADOR("ADMINISTRADOR"),
    USUARIO("CLIENTE"),
    ASESOR("ASESOR");

    private final String nombre;

    public static RolNombre from(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase()) {
            case "ADMINISTRADOR" -> ADMINISTRADOR;
            case "USUARIO"       -> USUARIO;
            case "ASESOR"        -> ASESOR;
            default -> throw new IllegalArgumentException("Rol desconocido: " + value);
        };
    }
}

