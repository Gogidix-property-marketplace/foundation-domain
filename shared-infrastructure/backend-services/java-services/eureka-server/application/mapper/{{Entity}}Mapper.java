package com.gogidix.infrastructure.discovery.application.mapper;

import com.gogidix.infrastructure.discovery.application.dto.DTO;
import com.gogidix.infrastructure.discovery.application.dto.CreateDTO;
import com.gogidix.infrastructure.discovery.application.dto.UpdateDTO;
import com.gogidix.infrastructure.discovery.application.dto.ResponseDTO;
import com.gogidix.infrastructure.discovery.domain.UserManagement.;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper interface for converting between Usermanagemen DTOs and domain objects.
 * Uses MapStruct for automatic mapping between layers.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface Mapper {

    Mapper INSTANCE = Mappers.getMapper(Mapper.class);

    /**
     * Converts  to ResponseDTO.
     *
     * @param UserManagement the domain entity
     * @return response DTO
     */
    @Mapping(source = "id.value", target = "id")
    @Mapping(source = "name.value", target = "name")
    ResponseDTO toResponseDTO( UserManagement);

    /**
     * Converts ResponseDTO to .
     *
     * @param responseDTO the response DTO
     * @return domain entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
     toDomain(ResponseDTO responseDTO);

    /**
     * Converts CreateDTO to .
     *
     * @param createDTO the creation DTO
     * @return domain entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "events", ignore = true)
     toDomain(CreateDTO createDTO);

    /**
     * Updates an existing  from UpdateDTO.
     *
     * @param updateDTO the update DTO
     * @param UserManagement the domain entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateDomain(UpdateDTO updateDTO, @MappingTarget  UserManagement);

    /**
     * Converts list of  to list of ResponseDTO.
     *
     * @param UserManagementList the domain entity list
     * @return list of response DTOs
     */
    List<ResponseDTO> toResponseDTOList(List<> UserManagementList);

    /**
     * Converts list of ResponseDTO to list of .
     *
     * @param responseDTOList the response DTO list
     * @return list of domain entities
     */
    List<> toDomainList(List<ResponseDTO> responseDTOList);

    /**
     * Converts DTO to .
     *
     * @param dto the entity DTO
     * @return domain entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
     toDomain(DTO dto);
}