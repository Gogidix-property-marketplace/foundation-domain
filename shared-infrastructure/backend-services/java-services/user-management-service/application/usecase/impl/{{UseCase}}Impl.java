package com.gogidix.infrastructure.usermgmt.application.usecase.impl;

import com.gogidix.infrastructure.usermgmt.application.usecase.;
import com.gogidix.infrastructure.usermgmt.application.dto.DTO;
import com.gogidix.infrastructure.usermgmt.application.dto.CreateDTO;
import com.gogidix.infrastructure.usermgmt.application.dto.UpdateDTO;
import com.gogidix.infrastructure.usermgmt.application.dto.ResponseDTO;
import com.gogidix.infrastructure.usermgmt.application.mapper.Mapper;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.service.DomainService;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.Id;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of  use case.
 * Handles Usermanagemen operations at the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Impl implements  {

    private final DomainService UserManagementDomainService;
    private final Mapper UsermanagemenMapper;

    @Override
    @Transactional
    public ResponseDTO create(CreateDTO createDTO) {
        log.info("Creating new Usermanagemen: {}", createDTO.getName());

        validateCreateDTO(createDTO);

         UserManagement = UserManagementDomainService.create(
                createDTO.getName(),
                createDTO.getDescription(),
                getCurrentUser()
        );

        ResponseDTO response = UsermanagemenMapper.toResponseDTO(UserManagement);

        log.info("Successfully created Usermanagemen: {} with ID: {}", UserManagement.getName().getValue(), UserManagement.getId());
        return response;
    }

    @Override
    @Transactional
    public ResponseDTO update(Long id, UpdateDTO updateDTO) {
        log.info("Updating Usermanagemen ID: {} by {}", id, getCurrentUser());

        validateUpdateDTO(updateDTO);

        Id UserManagementId = Id.fromString(id.toString());
         updated = UserManagementDomainService.update(
                UserManagementId,
                updateDTO.getName(),
                updateDTO.getDescription(),
                getCurrentUser()
        );

        ResponseDTO response = UsermanagemenMapper.toResponseDTO(updated);

        log.info("Successfully updated Usermanagemen ID: {}", id);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResponseDTO> findById(Long id) {
        log.debug("Finding Usermanagemen by ID: {}", id);

        try {
            Id UserManagementId = Id.fromString(id.toString());
            Optional<> UserManagement = UserManagementDomainService.findById(UserManagementId);

            return UserManagement.map(UsermanagemenMapper::toResponseDTO);
        } catch (Exception e) {
            log.warn("Error finding Usermanagemen by ID {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseDTO> findByStatus(String status) {
        log.debug("Finding Usermanagemen by status: {}", status);

        try {
            Status UserManagementStatus = Status.fromCode(status.toUpperCase());
            List<> UserManagementList = UserManagementDomainService.findByStatus(UserManagementStatus);

            return UserManagementList.stream()
                    .map(UsermanagemenMapper::toResponseDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status: {}", status);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseDTO> findAll() {
        log.debug("Finding all Usermanagemen entities");

        List<> UserManagementList = UserManagementDomainService.findAll();

        return UserManagementList.stream()
                .map(UsermanagemenMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseDTO> findAll(int page, int size) {
        log.debug("Finding all Usermanagemen entities with pagination: page={}, size={}", page, size);

        List<> UserManagementList = UserManagementDomainService.findAll(page, size);

        return UserManagementList.stream()
                .map(UsermanagemenMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseDTO> searchByName(String searchTerm) {
        log.debug("Searching Usermanagemen by name: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        List<> UserManagementList = UserManagementDomainService.searchByName(searchTerm.trim());

        return UserManagementList.stream()
                .map(UsermanagemenMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void activate(Long id) {
        log.info("Activating Usermanagemen ID: {}", id);

        Id UserManagementId = Id.fromString(id.toString());
        UserManagementDomainService.activate(UserManagementId, getCurrentUser());

        log.info("Successfully activated Usermanagemen ID: {}", id);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        log.info("Deactivating Usermanagemen ID: {}", id);

        Id UserManagementId = Id.fromString(id.toString());
        UserManagementDomainService.deactivate(UserManagementId, getCurrentUser());

        log.info("Successfully deactivated Usermanagemen ID: {}", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting Usermanagemen ID: {}", id);

        Id UserManagementId = Id.fromString(id.toString());
        UserManagementDomainService.delete(UserManagementId, getCurrentUser());

        log.info("Successfully deleted Usermanagemen ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCount() {
        return UserManagementDomainService.getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCountByStatus(String status) {
        try {
            Status UserManagementStatus = Status.fromCode(status.toUpperCase());
            return UserManagementDomainService.getCountByStatus(UserManagementStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status: {}", status);
            return 0L;
        }
    }

    /**
     * Validates the create DTO.
     *
     * @param createDTO the DTO to validate
     */
    private void validateCreateDTO(CreateDTO createDTO) {
        if (createDTO == null) {
            throw new IllegalArgumentException("CreateDTO cannot be null");
        }
        if (createDTO.getName() == null || createDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    /**
     * Validates the update DTO.
     *
     * @param updateDTO the DTO to validate
     */
    private void validateUpdateDTO(UpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new IllegalArgumentException("UpdateDTO cannot be null");
        }
        if (updateDTO.getName() == null || updateDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    /**
     * Gets the current user from security context.
     *
     * @return current user name
     */
    private String getCurrentUser() {
        // TODO: Implement proper security context integration
        return "system"; // Placeholder
    }
}