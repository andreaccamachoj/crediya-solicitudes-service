package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.r2dbc.entity.TipoPrestamoEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.math.BigInteger;

public interface TipoPrestamoReactiveRepository extends ReactiveCrudRepository<TipoPrestamoEntity, BigInteger>, ReactiveQueryByExampleExecutor<TipoPrestamoEntity> {
}
