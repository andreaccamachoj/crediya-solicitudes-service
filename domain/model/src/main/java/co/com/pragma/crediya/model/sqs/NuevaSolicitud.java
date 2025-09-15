package co.com.pragma.crediya.model.sqs;

public record NuevaSolicitud(
        Long idSolicitud,
        Double monto,
        Integer plazoMeses,
        Double tasaInteresAnualPorcentaje
) {}
