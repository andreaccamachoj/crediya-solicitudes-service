package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.r2dbc.entity.TipoPrestamoEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

public interface TipoPrestamoReactiveRepository extends ReactiveCrudRepository<TipoPrestamoEntity, BigInteger>, ReactiveQueryByExampleExecutor<TipoPrestamoEntity> {

    @Query("SELECT validacion_automatica FROM crediya.tipo_prestamo WHERE id_tipo_prestamo = :idTipoPrestamo")
    Mono<Boolean> isValidacionAutomaticaEnabled(Long idTipoPrestamo);
}
