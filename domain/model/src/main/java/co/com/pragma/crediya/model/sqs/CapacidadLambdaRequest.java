package co.com.pragma.crediya.model.sqs;

import java.util.List;

public record CapacidadLambdaRequest(
        Long idSolicitud,
        Long idUsuario,
        String nombres,
        String apellidos,
        String documentoIdentidad,
        String telefono,
        String email,
        Double ingresosTotales,
        NuevaSolicitud nuevoPrestamo,
        List<SolicitudesAprobadas> prestamosActivosAprobados
) {}
