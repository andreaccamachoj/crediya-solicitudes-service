package co.com.pragma.crediya.api.autenticacion;
import co.com.pragma.crediya.model.usuario.gateways.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String CTX_AUTHORIZATION = "CTX_AUTHORIZATION";
    private static final String ATTR_AUTH_USER = "authUser";
    private static final String CTX_USER = "AUTH_USER";

    private final UsuarioGateway usuarioGateway; // o TokenSesionGateway, según tu diseño

    private static String mask(String bearerOrJwt) {
        if (bearerOrJwt == null) return "null";
        String s = bearerOrJwt.startsWith("Bearer ") ? bearerOrJwt.substring(7) : bearerOrJwt;
        int n = Math.min(12, s.length());
        return s.substring(0, n) + "...";
    }

    @Override
    public Mono<Void> filter(ServerWebExchange ex, WebFilterChain chain) {
        String path1 = ex.getRequest().getPath().value();
        if (path1.startsWith("/swagger-ui")
                || path1.startsWith("/v3/api-docs")
                || path1.startsWith("/swagger-resources")
                || path1.startsWith("/webjars")) {
            return chain.filter(ex);
        }
        long t0 = System.nanoTime();
        var req = ex.getRequest();
        var method = req.getMethod();
        var path = req.getURI().getPath();

        // Log de entrada
        String incomingAuth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("[AUTH-IN] {} {} from={} authPresent={}",
                method, path, req.getRemoteAddress(), incomingAuth != null);

        // Deja pasar preflight CORS
        if (HttpMethod.OPTIONS.equals(method)) {
            log.debug("[AUTH] preflight OPTIONS bypass {}", path);
            return chain.filter(ex).doFinally(sig -> {
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                log.debug("[AUTH-DONE] {} {} -> {} (OPTIONS) in {}ms",
                        method, path, ex.getResponse().getStatusCode(), ms);
            });
        }

        if (incomingAuth == null || !incomingAuth.startsWith("Bearer ")) {
            log.warn("[AUTH] Missing/invalid Authorization header on {} {}", method, path);
            ex.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return ex.getResponse().setComplete()
                    .doFinally(sig -> {
                        long ms = (System.nanoTime() - t0) / 1_000_000L;
                        log.info("[AUTH-DONE] {} {} -> {} (no header) in {}ms",
                                method, path, ex.getResponse().getStatusCode(), ms);
                    });
        }

        String tokenMasked = mask(incomingAuth);

        return usuarioGateway.validarToken(incomingAuth) // pasa el "Bearer xxx" tal cual
                .doOnSubscribe(s -> log.info("[AUTH] Validando token {} ...", tokenMasked))
                .doOnNext(u -> {
                    try {
                        log.info("[AUTH] Token OK -> userId={} role={} nombrerol={}",
                                u.getIdUsuario(), u.getNombreRol(), u.getNombreRol());
                    } catch (Exception ignore) {
                        log.info("[AUTH] Token OK -> {}", String.valueOf(u));
                    }
                })
                .flatMap(u -> {
                    log.debug("[AUTH] authUser seteado en atributos; propagando token en Context");
                    ex.getAttributes().put(ATTR_AUTH_USER, u);
                    log.debug("[AUTH] authUser seteado en atributos; propagando token en Context");
                    return chain.filter(ex)
                            .contextWrite(ctx -> {
                                Context updated = ctx.put(CTX_AUTHORIZATION, incomingAuth);
                                updated = updated.put(CTX_USER, u);
                                return updated;
                            });

                })
                .doOnError(e -> log.warn("[AUTH] validarToken FALLÓ {} cause={}", tokenMasked, e.toString()))
                .onErrorResume(e -> {
                    System.out.println("error: " + e);
                    ex.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return ex.getResponse().setComplete();
                })
                .doFinally(sig -> {
                    long ms = (System.nanoTime() - t0) / 1_000_000L;
                    HttpStatus status = (HttpStatus) ex.getResponse().getStatusCode();
                    log.info("[AUTH-DONE] {} {} -> status={} signal={} in {}ms",
                            method, path, status, sig, ms);
                });
    }
}
