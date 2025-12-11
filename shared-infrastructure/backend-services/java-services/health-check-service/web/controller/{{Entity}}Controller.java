package com.gogidix.infrastructure.health.web.controller;

import com.gogidix.infrastructure.health.application.usecase.;
import com.gogidix.infrastructure.health.application.dto.ResponseDTO;
import com.gogidix.infrastructure.health.application.dto.CreateDTO;
import com.gogidix.infrastructure.health.application.dto.UpdateDTO;
import com.gogidix.infrastructure.health.web.dto.SuccessResponse;
import com.gogidix.infrastructure.health.web.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for Usermanagemen operations.
 * Provides HTTP endpoints for Usermanagemen management.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/Usermanagemen")
@RequiredArgsConstructor
public class Controller {

    private final  UsermanagemenUseCase;

    /**
     * Creates a new Usermanagemen.
     *
     * @param createDTO the creation data
     * @return created Usermanagemen response
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<ResponseDTO>> create(
            @Valid @RequestBody CreateDTO createDTO) {

        log.info("Creating new Usermanagemen: {}", createDTO.getName());

        ResponseDTO response = UsermanagemenUseCase.create(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.<ResponseDTO>builder()
                        .success(true)
                        .message(" created successfully")
                        .data(response)
                        .build());
    }

    /**
     * Updates an existing Usermanagemen.
     *
     * @param id        the Usermanagemen ID
     * @param updateDTO the update data
     * @return updated Usermanagemen response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<ResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDTO updateDTO) {

        log.info("Updating Usermanagemen ID: {}", id);

        ResponseDTO response = UsermanagemenUseCase.update(id, updateDTO);

        return ResponseEntity.ok(SuccessResponse.<ResponseDTO>builder()
                .success(true)
                .message(" updated successfully")
                .data(response)
                .build());
    }

    /**
     * Gets an Usermanagemen by ID.
     *
     * @param id the Usermanagemen ID
     * @return Usermanagemen response
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<ResponseDTO>> getById(@PathVariable Long id) {

        log.debug("Getting Usermanagemen by ID: {}", id);

        return UsermanagemenUseCase.findById(id)
                .map(response -> ResponseEntity.ok(SuccessResponse.<ResponseDTO>builder()
                        .success(true)
                        .data(response)
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets all Usermanagemen entities.
     *
     * @param page page number (0-based, default 0)
     * @param size page size (default 20)
     * @param sort sort field (default id)
     * @param direction sort direction (default DESC)
     * @return paginated Usermanagemen list
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<PagedResponse<ResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.debug("Getting all Usermanagemen entities - page: {}, size: {}, sort: {} {}", page, size, direction, sort);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        List<ResponseDTO> entities = UsermanagemenUseCase.findAll(page, size);

        // Note: In a real implementation, you'd get total count from the use case
        long totalElements = UsermanagemenUseCase.getCount();

        PagedResponse<ResponseDTO> pagedResponse = PagedResponse.<ResponseDTO>builder()
                .content(entities)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .first(page == 0)
                .last((page + 1) * size >= totalElements)
                .build();

        return ResponseEntity.ok(SuccessResponse.<PagedResponse<ResponseDTO>>builder()
                .success(true)
                .data(pagedResponse)
                .build());
    }

    /**
     * Gets Usermanagemen entities by status.
     *
     * @param status the status to filter by
     * @return list of Usermanagemen entities
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<List<ResponseDTO>>> getByStatus(@PathVariable String status) {

        log.debug("Getting Usermanagemen by status: {}", status);

        List<ResponseDTO> entities = UsermanagemenUseCase.findByStatus(status);

        return ResponseEntity.ok(SuccessResponse.<List<ResponseDTO>>builder()
                .success(true)
                .data(entities)
                .build());
    }

    /**
     * Searches Usermanagemen entities by name.
     *
     * @param searchTerm the search term
     * @return list of matching Usermanagemen entities
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<List<ResponseDTO>>> searchByName(
            @RequestParam String searchTerm) {

        log.debug("Searching Usermanagemen by name: {}", searchTerm);

        List<ResponseDTO> entities = UsermanagemenUseCase.searchByName(searchTerm);

        return ResponseEntity.ok(SuccessResponse.<List<ResponseDTO>>builder()
                .success(true)
                .data(entities)
                .build());
    }

    /**
     * Activates an Usermanagemen.
     *
     * @param id the Usermanagemen ID
     * @return success response
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Void>> activate(@PathVariable Long id) {

        log.info("Activating Usermanagemen ID: {}", id);

        UsermanagemenUseCase.activate(id);

        return ResponseEntity.ok(SuccessResponse.<Void>builder()
                .success(true)
                .message(" activated successfully")
                .build());
    }

    /**
     * Deactivates an Usermanagemen.
     *
     * @param id the Usermanagemen ID
     * @return success response
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Void>> deactivate(@PathVariable Long id) {

        log.info("Deactivating Usermanagemen ID: {}", id);

        UsermanagemenUseCase.deactivate(id);

        return ResponseEntity.ok(SuccessResponse.<Void>builder()
                .success(true)
                .message(" deactivated successfully")
                .build());
    }

    /**
     * Deletes an Usermanagemen.
     *
     * @param id the Usermanagemen ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable Long id) {

        log.info("Deleting Usermanagemen ID: {}", id);

        UsermanagemenUseCase.delete(id);

        return ResponseEntity.ok(SuccessResponse.<Void>builder()
                .success(true)
                .message(" deleted successfully")
                .build());
    }

    /**
     * Gets Usermanagemen statistics.
     *
     * @return statistics response
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Object>> getStatistics() {

        log.debug("Getting Usermanagemen statistics");

        long totalCount = UsermanagemenUseCase.getCount();
        long activeCount = UsermanagemenUseCase.getCountByStatus("ACTIVE");
        long inactiveCount = UsermanagemenUseCase.getCountByStatus("INACTIVE");

        Object statistics = java.util.Map.of(
                "totalCount", totalCount,
                "activeCount", activeCount,
                "inactiveCount", inactiveCount,
                "otherCount", totalCount - activeCount - inactiveCount
        );

        return ResponseEntity.ok(SuccessResponse.builder()
                .success(true)
                .data(statistics)
                .build());
    }
}