package co.com.pragma.crediya.api.mapper;

import co.com.pragma.crediya.api.dto.request.CrearSolicitudRequest;
import co.com.pragma.crediya.api.dto.response.SolicitudResponse;
import co.com.pragma.crediya.model.solicitud.Solicitud;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;


@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface SolicitudMapper {

    @Mapping(target = "idEstado", ignore = true)
    @Mapping(target = "idUsuario", ignore = true)
    Solicitud toDomain(CrearSolicitudRequest crearSolicitudRequest);

    SolicitudResponse toResponse(Solicitud solicitud);
}
