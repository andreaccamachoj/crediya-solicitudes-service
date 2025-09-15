package co.com.pragma.crediya.model.sqs;

public record SolicitudesAprobadas(
        Long idSolicitud,
        Double monto,
        Integer plazoMeses,
        Double tasaInteresAnualPorcentaje
) {}
