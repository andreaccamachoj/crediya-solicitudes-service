package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.model.solicitud.SolicitudDetalle;
import co.com.pragma.crediya.r2dbc.entity.SolicitudEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.List;

public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, BigInteger>, ReactiveQueryByExampleExecutor<SolicitudEntity> {

    @Query("""
      SELECT DISTINCT s.id_usuario
      FROM crediya.solicitud s
      JOIN crediya.estados e ON s.id_estado = e.id_estado
      WHERE e.nombre IN (:estados)
      AND (:email IS NULL OR s.email = :email)
      """)
    Flux<Long> findDistinctIdUsuariosByEstadosByFilter(
            @Param("estados") List<String> estados,
            @Param("email") String email
    );

    @Query("""
       SELECT s.id_solicitud AS idSolicitud,
              s.monto,
              s.plazo,
              s.email,
              s.id_usuario AS idUsuario,
              tp.nombre AS tipoPrestamo,
              tp.tasa_interes AS tasaInteres,
              e.nombre AS estado
       FROM crediya.solicitud s
       JOIN crediya.estados e ON s.id_estado = e.id_estado
       JOIN crediya.tipo_prestamo tp ON s.id_tipo_prestamo = tp.id_tipo_prestamo
       WHERE e.nombre IN (:estados)
         AND s.id_usuario IN (:usuarios)
       ORDER BY s.id_solicitud
       """)
    Flux<SolicitudDetalle> findDetallesByEstadosAndUsuarios(@Param("estados") List<String> estados, @Param("usuarios") List<Long> usuarios);

    @Query("""
       SELECT s.id_solicitud AS idSolicitud,
              s.monto,
              s.plazo,
              s.email,
              s.id_usuario AS idUsuario,
              tp.nombre AS tipoPrestamo,
              tp.tasa_interes AS tasaInteres,
              e.nombre AS estado
       FROM crediya.solicitud s
       JOIN crediya.estados e ON s.id_estado = e.id_estado
       JOIN crediya.tipo_prestamo tp ON s.id_tipo_prestamo = tp.id_tipo_prestamo
       WHERE e.nombre = 'APROBADO'
         AND s.id_usuario IN (:usuarios)
       """)
    Flux<SolicitudDetalle> findSolicitudesAprobadasByUsuarios(@Param("usuarios") List<Long> usuarios);

    @Query("""
           SELECT COUNT(*)
           FROM crediya.solicitud s
           JOIN crediya.estados e ON s.id_estado = e.id_estado
           WHERE e.nombre IN (:estados)
             AND s.id_usuario IN (:usuarios)
           """)
    Mono<Long> countByEstadosAndUsuarios(@Param("estados") List<String> estados, @Param("usuarios") List<Long> usuarios);
}