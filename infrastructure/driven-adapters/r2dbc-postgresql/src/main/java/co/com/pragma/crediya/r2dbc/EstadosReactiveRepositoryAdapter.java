package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.model.estados.Estados;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.r2dbc.entity.EstadosEntity;
import co.com.pragma.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

@Repository
@Transactional
public class EstadosReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Estados,
        EstadosEntity,
        BigInteger,
        EstadosReactiveRepository
> implements EstadosRepository {
    public EstadosReactiveRepositoryAdapter(EstadosReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Estados.class));
    }

    private static final Logger log = LoggerFactory.getLogger(EstadosReactiveRepositoryAdapter.class);

    @Override
    public Mono<Boolean> existsById(Long id) {
        return repository.existsById(BigInteger.valueOf(id));
    }

    @Override
    public Mono<Long> findIdByNombre(String nombre) {
        return repository.findIdByNombre(nombre).doOnSubscribe(s -> log.info("[DB] findIdByNombre({})", nombre))
                .doOnNext(id -> log.debug("[DB] estado {} -> id={}", nombre, id))
                .doOnError(e -> log.error("[DB] findIdByNombre error nombre={}", nombre, e));
    }
}
