package co.com.pragma.crediya.r2dbc.tx;

import co.com.pragma.crediya.model.gateway.ReactiveTxGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ReactiveTxAdapter implements ReactiveTxGateway {

    private final TransactionalOperator txOperator;

    @Override
    public <T> Mono<T> required(Mono<T> work) {
        return txOperator.transactional(work);
    }
}
