package com.gogidix.infrastructure.registry.application.service;

import com.gogidix.infrastructure.registry.application.dto.ResponseDTO;

import java.util.List;
import java.util.Optional;

/**
 * Application service interface for Usermanagemen operations.
 * Provides high-level business operations for the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface  {

    /**
     * Service method for processing Usermanagemen business logic.
     *
     * @param UsermanagemenId the Usermanagemen identifier
     * @return processed Usermanagemen response
     */
    ResponseDTO process(String UsermanagemenId);

    /**
     * Validates Usermanagemen data.
     *
     * @param UsermanagemenId the Usermanagemen identifier
     * @return validation result
     */
    boolean validate(String UsermanagemenId);

    /**
     * Gets all active Usermanagemen entities.
     *
     * @return list of active Usermanagemen entities
     */
    List<ResponseDTO> getActive();

    /**
     * Finds Usermanagemen by business key.
     *
     * @param businessKey the business key
     * @return optional Usermanagemen response
     */
    Optional<ResponseDTO> findByBusinessKey(String businessKey);
}