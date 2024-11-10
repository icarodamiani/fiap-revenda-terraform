package io.fiap.revenda.driven.core.domain.mapper;

import io.fiap.revenda.driven.core.domain.Documento;
import io.fiap.revenda.driver.controller.dto.DocumentoDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentoMapper extends BaseMapper<DocumentoDTO, Documento> {
}
