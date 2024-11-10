package io.fiap.revenda.driven.repository;


import io.fiap.revenda.driven.core.domain.Documento;
import io.fiap.revenda.driven.core.domain.ImmutableDocumento;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class DocumentoRepository {
    private static final String TABLE_NAME = "detran_tb";

    private final DynamoDbAsyncClient client;

    public DocumentoRepository(DynamoDbAsyncClient client) {
        this.client = client;
    }

    public Mono<Void> save(Documento documento) {
        var atributos = new HashMap<String, AttributeValueUpdate>();
        atributos.put("WEBHOOK",
            AttributeValueUpdate.builder().value(v -> v.s(documento.getWebhook()).build()).build());

        if (documento.getEmitido() != null) {
            atributos.put("EMITIDO",
                AttributeValueUpdate.builder().value(v -> v.s(documento.getEmitido().toString()).build()).build());
        }

        var request = UpdateItemRequest.builder()
            .attributeUpdates(atributos)
            .tableName(TABLE_NAME)
            .key(Map.of("ID", AttributeValue.fromS(documento.getId())))
            .build();

        return Mono.fromFuture(client.updateItem(request))
            .then();
    }

    public Mono<Documento> findById(String id) {
        var request = QueryRequest.builder()
            .tableName(TABLE_NAME)
            .keyConditionExpression("#id = :id")
            .expressionAttributeNames(Map.of("#id", "ID"))
            .expressionAttributeValues(Map.of(":id", AttributeValue.fromS(id)))
            .build();

        return Mono.fromFuture(client.query(request))
            .filter(QueryResponse::hasItems)
            .map(response -> response.items().get(0))
            .map(this::convertItem);
    }

    private Documento convertItem(Map<String, AttributeValue> item) {
        var documento = ImmutableDocumento.builder()
            .id(item.get("ID").s())
            .webhook(item.get("WEBHOOK").s());

        if (item.containsKey("EMITIDO") && StringUtils.hasText(item.get("EMITIDO").s())) {
            documento.emitido(Boolean.valueOf(item.get("EMITIDO").s()));
        }

        return documento.build();
    }
}
