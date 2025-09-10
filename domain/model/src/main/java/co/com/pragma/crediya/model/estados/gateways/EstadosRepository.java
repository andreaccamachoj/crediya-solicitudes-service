package co.com.pragma.crediya.model.estados.gateways;

import reactor.core.publisher.Mono;

public interface EstadosRepository {
    Mono<Boolean> existsById(Long id);
    Mono<Long> findIdByNombre(String nombre);
}
