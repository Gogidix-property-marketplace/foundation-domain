package com.gogidix.ai.tagging.application.query.impl;

import com.gogidix.ai.tagging.application.query.;
import com.gogidix.ai.tagging.application.dto.ResponseDTO;
import com.gogidix.ai.tagging.application.mapper.Mapper;
import com.gogidix.ai.tagging.domain.UserManagement.repository.Repository;
import com.gogidix.ai.tagging.domain.UserManagement.;
import com.gogidix.ai.tagging.domain.UserManagement.Id;
import com.gogidix.ai.tagging.domain.UserManagement.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of  use case.
 * Handles read operations for Usermanagemen data.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Impl implements  {

    private final Repository UserManagementRepository;
    private final Mapper UsermanagemenMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ResponseDTO> findById(Long id) {
        log.debug("Finding Usermanagemen by ID: {}", id);

        try {
            Id UserManagementId = Id.fromString(id.toString());
            Optional<> UserManagement = UserManagementRepository.findById(UserManagementId);

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
            List<> UserManagementList = UserManagementRepository.findByStatus(UserManagementStatus);

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
    public List<ResponseDTO> searchByName(String searchTerm) {
        log.debug("Searching Usermanagemen by name: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        List<> UserManagementList = UserManagementRepository.searchByDescription(searchTerm.trim());

        return UserManagementList.stream()
                .map(UsermanagemenMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseDTO> findAll(int page, int size) {
        log.debug("Finding all Usermanagemen with pagination: page={}, size={}", page, size);

        List<> UserManagementList = UserManagementRepository.findAll(page, size);

        return UserManagementList.stream()
                .map(UsermanagemenMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        log.debug("Counting all Usermanagemen entities");

        return UserManagementRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        log.debug("Counting Usermanagemen by status: {}", status);

        try {
            Status UserManagementStatus = Status.fromCode(status.toUpperCase());
            return UserManagementRepository.countByStatus(UserManagementStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status: {}", status);
            return 0L;
        }
    }
}