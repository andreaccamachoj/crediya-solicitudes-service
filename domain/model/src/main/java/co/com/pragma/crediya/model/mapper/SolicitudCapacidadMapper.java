package co.com.pragma.crediya.model.mapper;
import co.com.pragma.crediya.model.solicitud.SolicitudDetalle;
import co.com.pragma.crediya.model.sqs.CapacidadLambdaRequest;
import co.com.pragma.crediya.model.sqs.NuevaSolicitud;
import co.com.pragma.crediya.model.sqs.SolicitudesAprobadas;
import co.com.pragma.crediya.model.usuario.UsuarioDemografico;

import java.util.List;

public final class SolicitudCapacidadMapper {

    private SolicitudCapacidadMapper() { }

    public static SolicitudesAprobadas toSolicitudAprobada(SolicitudDetalle sd) {
        return new SolicitudesAprobadas(
                safeLong(sd.getIdsolicitud()),
                safeDouble(sd.getMonto()),
                safeInt(sd.getPlazo()),
                safeDouble(sd.getTasainteres())
        );
    }

    public static NuevaSolicitud toNuevaSolicitud(SolicitudDetalle detalle) {
        return new NuevaSolicitud(
                safeLong(detalle.getIdsolicitud()),
                safeDouble(detalle.getMonto()),
                safeInt(detalle.getPlazo()),
                safeDouble(detalle.getTasainteres())
        );
    }

    public static CapacidadLambdaRequest toCapacidadLambdaRequest(
            Long idSolicitud,
            UsuarioDemografico usuario,
            NuevaSolicitud nuevoPrestamo,
            List<SolicitudesAprobadas> activos
    ) {
        return new CapacidadLambdaRequest(
                idSolicitud,
                usuario.idUsuario(),
                usuario.nombres(),
                usuario.apellidos(),
                usuario.documentoIdentidad(),
                usuario.telefono(),
                usuario.correoElectronico(),
                usuario.salarioBase(),
                nuevoPrestamo,
                activos
        );
    }

    public static Double safeDouble(Double value) { return value != null ? value : 0.0; }
    public static Long safeLong(Long value) { return value != null ? value : 0L; }
    public static Integer safeInt(Integer value) { return value != null ? value : 0; }
}
