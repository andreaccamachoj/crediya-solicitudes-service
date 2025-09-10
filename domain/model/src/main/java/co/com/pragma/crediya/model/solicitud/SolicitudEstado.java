package co.com.pragma.crediya.model.solicitud;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SolicitudEstado {

    Long idSolicitud;
    String estado;
}
