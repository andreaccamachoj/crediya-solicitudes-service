package co.com.pragma.crediya.r2dbc.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "solicitud")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SolicitudEntity {
    @Id
    @Column("id_solicitud")
    private Long idSolicitud;

    private Double monto;

    private Integer plazo;

    private String email;

    @Column("id_estado")
    private Long idEstado;

    @Column("id_tipo_prestamo")
    private Long idTipoPrestamo;

    @Column("id_usuario")
    private Long idUsuario;
}
