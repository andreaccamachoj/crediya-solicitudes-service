package co.com.pragma.crediya.model.sqs.gateways;

import co.com.pragma.crediya.model.sqs.ReportesSolicitudesAprobadas;
import reactor.core.publisher.Mono;

public interface AprobacionEventPublisher {
    public Mono<String> send(ReportesSolicitudesAprobadas idSolicitud);

}
