package co.com.pragma.crediya.model.sqs;

public record ReportesSolicitudesAprobadas(
        Long idSolicitud,
        Long idUsuario,
        Double monto,
        String estado
) {}
