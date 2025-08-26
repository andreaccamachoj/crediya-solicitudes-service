package co.com.pragma.crediya.model.usuario.gateways;

import co.com.pragma.crediya.model.usuario.Usuario;
import reactor.core.publisher.Mono;

public interface UsuarioGateway {
    Mono<Usuario> existsByDocumentoIdentidad(String documentoIdentidad);
}
