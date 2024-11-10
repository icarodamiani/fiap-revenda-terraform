package io.fiap.revenda.driver.controller;

import static org.slf4j.LoggerFactory.getLogger;

import io.fiap.revenda.driven.core.domain.mapper.DocumentoMapper;
import io.fiap.revenda.driven.core.exception.HttpStatusExceptionConverter;
import io.fiap.revenda.driven.core.service.DocumentoService;
import io.fiap.revenda.driver.controller.dto.DocumentoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/detran/documentos", produces = MediaType.APPLICATION_JSON_VALUE)
public class DetranDocumentoController {
    private static final Logger LOGGER = getLogger(DetranDocumentoController.class);
    private final DocumentoMapper mapper;
    private final DocumentoService documentoService;
    private final HttpStatusExceptionConverter httpStatusExceptionConverter;

    public DetranDocumentoController(DocumentoMapper mapper,
                                     DocumentoService documentoService,
                                     HttpStatusExceptionConverter httpStatusExceptionConverter) {
        this.mapper = mapper;
        this.documentoService = documentoService;
        this.httpStatusExceptionConverter = httpStatusExceptionConverter;
    }

    @PostMapping("/emitir")
    @Operation(description = "Persiste um registro de documento a ser emitido")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Opened"),
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public Mono<ResponseEntity<DocumentoDTO>> emitir(@RequestBody DocumentoDTO payment) {
        return documentoService.save(mapper.domainFromDto(payment))
            .map(mapper::dtoFromDomain)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .onErrorMap(e ->
                new ResponseStatusException(httpStatusExceptionConverter.convert(e), e.getMessage(), e))
            .doOnError(throwable -> LOGGER.error(throwable.getMessage(), throwable));
    }
}
