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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RestConsumer implements UsuarioGateway {

    private static final Logger log = LoggerFactory.getLogger(RestConsumer.class);
    private final WebClient client;

    @Override
    public Mono<Usuario> existsByDocumentoIdentidad(String documentoIdentidad) {
        log.info("Iniciando búsqueda de usuario por documento: {}", documentoIdentidad);
        return client
                .get()
                .uri("/usuario/" + documentoIdentidad)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Usuario.class)
                .doOnNext(usuario -> log.info("Usuario encontrado exitosamente para el documento: {}", documentoIdentidad))
                .doOnError(e -> log.error("Error al buscar usuario con documento {}:", documentoIdentidad, e));
    }

    public Mono<UsuarioAutenticado> validarToken(String jwtOrBearer) {
        log.info("Iniciando validación de token de autorización.");
        String bearer = (jwtOrBearer != null && jwtOrBearer.startsWith("Bearer "))
                ? jwtOrBearer
                : "Bearer " + jwtOrBearer;
        return client.get()
                .uri("/validate")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(UsuarioAutenticado.class)
                .doOnNext(usuarioAuth -> log.info("Token validado exitosamente para el usuario: {}", usuarioAuth.getIdUsuario()))
                .doOnError(e -> log.warn("Falló la validación del token: {}", e.getMessage()));
    }

    @Override
    public Flux<UsuarioDemografico> findAllByIds(List<Long> ids) {
        List<Long> clean = (ids == null) ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();

        if (clean.isEmpty()) return Flux.empty();

        return client.post()
                .uri("/usuario/listUsuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(clean)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UsuarioDemografico>>() {})
                .flatMapMany(Flux::fromIterable)
                .doOnSubscribe(s -> log.info("[AUTH→] POST /usuarios/batch size={}", clean.size()))
                .doOnNext(u -> log.debug("[AUTH→] usuario id={} email={}", u.idUsuario(), u.correoElectronico()))
                .doOnError(e -> log.warn("[AUTH→] error {}", e.toString()));
    }

}
