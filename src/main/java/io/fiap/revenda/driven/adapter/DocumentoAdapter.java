package io.fiap.revenda.driven.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.fiap.revenda.driven.core.domain.Documento;
import io.fiap.revenda.driven.core.exception.BadRequestException;
import io.fiap.revenda.driven.core.messaging.MessagingPort;
import io.fiap.revenda.driven.core.port.DocumentoPort;
import io.fiap.revenda.driven.repository.DocumentoRepository;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import io.vavr.Function1;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class DocumentoAdapter implements DocumentoPort {

    private final MessagingPort messagingPort;
    private final DocumentoRepository repository;
    private final ObjectMapper objectMapper;

    private final String queue;

    public DocumentoAdapter(MessagingPort messagingPort,
                            DocumentoRepository repository,
                            ObjectMapper objectMapper,
                            @Value("${aws.sqs.emitirDocumentoQueue.queue:detran_documentos_emitir_queue}")
                            String queue) {
        this.messagingPort = messagingPort;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.queue = queue;
    }

    @Override
    public Mono<Documento> save(Documento documento) {
        return repository.save(documento)
            .then(this.sendToAsyncProcessing(documento));
    }

    @Override
    public Mono<Void> update(String id, String operations) {
        return repository.findById(id)
            .map(documento -> applyPatch().unchecked().apply(documento, operations))
            .flatMap(repository::save)
            .onErrorMap(JsonPatchException.class::isInstance, BadRequestException::new);
    }

    @Override
    public Mono<Documento> findById(String id) {
        return repository.findById(id)
            .onErrorMap(JsonPatchException.class::isInstance, BadRequestException::new);
    }

    private CheckedFunction2<Documento, String, Documento> applyPatch() {
        return (documento, operations) -> {
            var patch = readOperations()
                .unchecked()
                .apply(operations);

            var patched = patch.apply(objectMapper.convertValue(documento, JsonNode.class));

            return objectMapper.treeToValue(patched, Documento.class);
        };
    }

    private CheckedFunction1<String, JsonPatch> readOperations() {
        return operations -> {
            final InputStream in = new ByteArrayInputStream(operations.getBytes());
            return objectMapper.readValue(in, JsonPatch.class);
        };
    }

    @Override
    public Flux<Message> readEmitirDocumento(Function1<Documento, Mono<Documento>> handle) {
        return messagingPort.read(queue, handle, readEvent());
    }

    private CheckedFunction1<Message, Documento> readEvent() {
        return message -> objectMapper.readValue(message.body(), Documento.class);
    }


    public Mono<Documento> sendToAsyncProcessing(Documento documento) {
        return messagingPort.send(queue, documento, serializePayload());
    }

    private <T> CheckedFunction1<T, String> serializePayload() {
        return objectMapper::writeValueAsString;
    }

}
