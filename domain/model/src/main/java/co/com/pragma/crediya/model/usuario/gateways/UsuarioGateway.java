package co.com.pragma.crediya.model.usuario.gateways;

import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.UsuarioDemografico;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UsuarioGateway {
    Mono<Usuario> existsByDocumentoIdentidad(String documentoIdentidad);
    public Mono<UsuarioAutenticado> validarToken(String jwtToken);
    Flux<UsuarioDemografico> findAllByIds(List<Long> ids);
}
