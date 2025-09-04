package co.com.pragma.crediya.consumer;

import co.com.pragma.crediya.model.autenticacion.UsuarioAutenticado;
import co.com.pragma.crediya.model.usuario.Usuario;
import co.com.pragma.crediya.model.usuario.UsuarioDemografico;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestConsumer implements UsuarioGateway {

    private static final Logger log = LoggerFactory.getLogger(RestConsumer.class);
    private final WebClient client;

    @Override
    public Mono<Usuario> existsByDocumentoIdentidad(String documentoIdentidad) {
        return client
                .get()
                .uri("/usuario/" + documentoIdentidad)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Usuario.class);
    }

    public Mono<UsuarioAutenticado> validarToken(String jwtOrBearer) {
        String bearer = (jwtOrBearer != null && jwtOrBearer.startsWith("Bearer "))
                ? jwtOrBearer
                : "Bearer " + jwtOrBearer;
        return client.get()
                .uri("/validate") // asegúrate que coincide con loginPath.getValidateToken()
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(UsuarioAutenticado.class);
    }

    @Override
    public Flux<UsuarioDemografico> findAllByIds(List<Long> ids) {
        List<Long> clean = (ids == null) ? List.of()
                : ids.stream().filter(i -> i != null).distinct().toList();

        if (clean.isEmpty()) return Flux.empty();

        return client.post()
                .uri("/usuario/listUsuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(clean)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UsuarioDemografico>>() {}) // Mono<List>
                .flatMapMany(Flux::fromIterable) // convertimos la lista en Flux
                .doOnSubscribe(s -> log.info("[AUTH→] POST /usuarios/batch size={}", clean.size()))
                .doOnNext(u -> log.debug("[AUTH→] usuario id={} email={}", u.idUsuario(), u.correoElectronico()))
                .doOnError(e -> log.warn("[AUTH→] error {}", e.toString()));
    }

}
