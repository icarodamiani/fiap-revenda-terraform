package io.fiap.revenda.driven.core.domain.mapper;

import java.util.List;

public interface BaseMapper<DTO, DOMAIN> {
    DOMAIN domainFromDto(DTO dto);

    DTO dtoFromDomain(DOMAIN domain);

    List<DTO> dtoListFromDomainArray(DOMAIN[] domainList);
}
