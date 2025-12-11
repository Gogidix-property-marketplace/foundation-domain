package com.gogidix.platform.common.persistence.specification;

import com.gogidix.platform.common.persistence.entity.BaseEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Base specifications for common JPA queries
 */
@Component
public class BaseSpecification {

    /**
     * Specification for active entities (not soft deleted)
     */
    public static <T extends BaseEntity> Specification<T> isActive() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("deleted"), false);
    }

    /**
     * Specification for entities with specific tenant ID
     */
    public static <T extends BaseEntity> Specification<T> hasTenantId(String tenantId) {
        return (root, query, criteriaBuilder) -> {
            if (tenantId == null) {
                return criteriaBuilder.isNull(root.get("tenantId"));
            }
            return criteriaBuilder.equal(root.get("tenantId"), tenantId);
        };
    }

    /**
     * Specification for entities created by specific user
     */
    public static <T extends BaseEntity> Specification<T> createdBy(String createdBy) {
        return (root, query, criteriaBuilder) -> {
            if (createdBy == null) {
                return criteriaBuilder.isNull(root.get("createdBy"));
            }
            return criteriaBuilder.equal(root.get("createdBy"), createdBy);
        };
    }

    /**
     * Specification for entities updated by specific user
     */
    public static <T extends BaseEntity> Specification<T> updatedBy(String updatedBy) {
        return (root, query, criteriaBuilder) -> {
            if (updatedBy == null) {
                return criteriaBuilder.isNull(root.get("updatedBy"));
            }
            return criteriaBuilder.equal(root.get("updatedBy"), updatedBy);
        };
    }

    /**
     * Specification for entities created within date range
     */
    public static <T extends BaseEntity> Specification<T> createdAtBetween(Instant startDate, Instant endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
        };
    }

    /**
     * Specification for entities updated within date range
     */
    public static <T extends BaseEntity> Specification<T> updatedAtBetween(Instant startDate, Instant endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), startDate);
            }
            return criteriaBuilder.between(root.get("updatedAt"), startDate, endDate);
        };
    }

    /**
     * Specification for entities with ID in list
     */
    public static <T extends BaseEntity> Specification<T> idIn(java.util.List<UUID> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.disjunction();
            }
            return root.get("id").in(ids);
        };
    }

    /**
     * Specification for text search in specified field (case-insensitive)
     */
    public static <T> Specification<T> containsText(String fieldName, String text) {
        return (root, query, criteriaBuilder) -> {
            if (text == null || text.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(fieldName)),
                "%" + text.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification for exact match in specified field
     */
    public static <T> Specification<T> equalsField(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.isNull(root.get(fieldName));
            }
            return criteriaBuilder.equal(root.get(fieldName), value);
        };
    }

    /**
     * Specification for field in values list
     */
    public static <T> Specification<T> fieldIn(String fieldName, java.util.Collection<?> values) {
        return (root, query, criteriaBuilder) -> {
            if (values == null || values.isEmpty()) {
                return criteriaBuilder.disjunction();
            }
            return root.get(fieldName).in(values);
        };
    }

    /**
     * Combine multiple specifications with AND logic
     */
    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specifications) {
        Specification<T> result = Specification.where(null);
        for (Specification<T> spec : specifications) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    /**
     * Combine multiple specifications with OR logic
     */
    @SafeVarargs
    public static <T> Specification<T> or(Specification<T>... specifications) {
        Specification<T> result = Specification.where(null);
        for (Specification<T> spec : specifications) {
            if (spec != null) {
                if (result == null) {
                    result = spec;
                } else {
                    result = result.or(spec);
                }
            }
        }
        return result;
    }
}