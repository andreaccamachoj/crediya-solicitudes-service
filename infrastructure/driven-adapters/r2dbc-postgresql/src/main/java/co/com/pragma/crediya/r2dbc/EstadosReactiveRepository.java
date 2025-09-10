package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.r2dbc.entity.EstadosEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

public interface EstadosReactiveRepository extends ReactiveCrudRepository<EstadosEntity, BigInteger>, ReactiveQueryByExampleExecutor<EstadosEntity> {


    @Query("SELECT id_estado FROM estados WHERE nombre = :nombre")
    Mono<Long> findIdByNombre(@Param("nombre") String nombre);

}
