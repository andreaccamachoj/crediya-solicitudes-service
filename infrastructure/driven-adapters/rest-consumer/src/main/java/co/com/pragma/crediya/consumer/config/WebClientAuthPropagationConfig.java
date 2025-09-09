package co.com.pragma.crediya.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientAuthPropagationConfig {

    @Bean
    public ExchangeFilterFunction propagateAuthHeader() {
        return (request, next) -> Mono.deferContextual(ctx -> {
            String auth = ctx.<String>getOrEmpty("CTX_AUTHORIZATION")
                    .orElseGet(() -> ctx.<ServerWebExchange>getOrEmpty(ServerWebExchange.class)
                            .map(ex -> ex.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                            .orElse(null));

            if (auth == null || request.headers().getFirst(HttpHeaders.AUTHORIZATION) != null) {
                return next.exchange(request);
            }
            ClientRequest mutated = ClientRequest.from(request)
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .build();
            return next.exchange(mutated);
        });
    }


}


