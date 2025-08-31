package co.com.pragma.crediya.consumer;

import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RestConsumer implements UsuarioGateway {
    private final WebClient client;
    @Override
    public Mono<Usuario> existsByDocumentoIdentidad(String documentoIdentidad) {
        return client
                .get()
                .uri("/" + documentoIdentidad)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Usuario.class);
    }
}
