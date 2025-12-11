package com.gogidix.ai.pricing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Usermanagemen responses.
 * Used for returning Usermanagemen data to clients.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {

    private String id;
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private Long version;
    private List<String> tags;
    private Object metadata;
}