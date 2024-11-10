package io.fiap.revenda.driven.core.service;

import io.fiap.revenda.driven.core.domain.Documento;
import io.fiap.revenda.driven.core.domain.ImmutableDocumento;
import io.fiap.revenda.driven.core.port.DocumentoPort;
import io.vavr.Function1;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Service
public class DocumentoService {


    private final DocumentoPort documentoPort;
    private final WebClient webClient;

    public DocumentoService(DocumentoPort documentoPort, WebClient webClient) {
        this.documentoPort = documentoPort;
        this.webClient = webClient;
    }

    public Mono<Documento> save(Documento documento) {
        return documentoPort.save(documento);
    }

    public Flux<Message> handleEvent() {
        return documentoPort.readEmitirDocumento(handle());
    }

    private Function1<Documento, Mono<Documento>> handle() {
        return documento -> documentoPort.update(documento.getId(),
                "[{\"op\": \"replace\",\"path\": \"/emitido\",\"value\": \"true\"}]")
            .then(this.notify(documento).map(unused -> documento));
    }

    private Mono<ResponseEntity<Void>> notify(Documento documento) {
        return webClient.post()
            .uri(URI.create(documento.getWebhook()))
            .body(BodyInserters.fromValue(ImmutableDocumento.copyOf(documento).withEmitido(true)))
            .retrieve()
            .toBodilessEntity();
    }
}
