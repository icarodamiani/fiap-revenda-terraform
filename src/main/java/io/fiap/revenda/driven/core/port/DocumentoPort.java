package io.fiap.revenda.driven.core.port;

import io.fiap.revenda.driven.core.domain.Documento;
import io.vavr.Function1;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

public interface DocumentoPort {

    Mono<Documento> save(Documento order);

    Mono<Documento> findById(String id);

    Mono<Void> update(String id, String operations);

    Flux<Message> readEmitirDocumento(Function1<Documento, Mono<Documento>> handle);

}
