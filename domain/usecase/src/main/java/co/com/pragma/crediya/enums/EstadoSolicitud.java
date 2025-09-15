package co.com.pragma.crediya.enums;

import java.util.Arrays;
import java.util.Optional;

public enum EstadoSolicitud {
    APROBADO("APROBADO"),
    RECHAZADO("RECHAZADO"),
    REVISION_MANUAL("REVISION MANUAL");

    private final String nombre;

    EstadoSolicitud(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve el enum si la decisión coincide (ignorando mayúsculas/minúsculas).
     * Si no hay coincidencia, retorna Optional.empty()
     */
    public static Optional<EstadoSolicitud> fromDecision(String decision) {
        return Arrays.stream(values())
                .filter(e -> e.getNombre().equalsIgnoreCase(decision))
                .findFirst();
    }
}