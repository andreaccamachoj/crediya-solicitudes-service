package co.com.pragma.crediya.model.tipoprestamo.gateways;

import reactor.core.publisher.Mono;

public interface TipoPrestamoRepository {
    Mono<Boolean> existsById(Long id);
    Mono<Boolean> isValidacionAutomaticaEnabled(Long idTipoPrestamo);
}
