package co.com.pragma.crediya.r2dbc;

import co.com.pragma.crediya.model.estados.Estados;
import co.com.pragma.crediya.model.estados.gateways.EstadosRepository;
import co.com.pragma.crediya.r2dbc.entity.EstadosEntity;
import co.com.pragma.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
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

    @Override
    public Mono<Boolean> existsById(Long id) {
        return repository.existsById(BigInteger.valueOf(id));
    }
}
