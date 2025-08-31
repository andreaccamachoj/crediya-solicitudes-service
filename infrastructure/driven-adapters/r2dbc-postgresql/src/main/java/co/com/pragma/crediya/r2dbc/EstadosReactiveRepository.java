package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.r2dbc.entity.EstadosEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.math.BigInteger;

public interface EstadosReactiveRepository extends ReactiveCrudRepository<EstadosEntity, BigInteger>, ReactiveQueryByExampleExecutor<EstadosEntity> {

}
