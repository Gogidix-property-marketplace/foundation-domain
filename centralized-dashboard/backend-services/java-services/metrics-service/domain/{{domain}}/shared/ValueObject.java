package com.gogidix.dashboard.metrics.domain.UserManagement.shared;

import java.util.Objects;

/**
 * Base interface for all value objects.
 * Value objects are immutable objects defined by their attributes rather than identity.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface ValueObject {

    /**
     * Checks if this value object has the same value as another.
     *
     * @param other the other value object to compare with
     * @return true if values are equal, false otherwise
     */
    boolean sameValueAs(ValueObject other);

    /**
     * Default implementation for equals based on value equality.
     *
     * @param obj the object to compare with
     * @return true if equal, false otherwise
     */
    default boolean valueEquals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        return sameValueAs((ValueObject) obj);
    }

    /**
     * Default implementation for hashCode based on value.
     *
     * @return hash code
     */
    default int valueHashCode() {
        return Objects.hash(this);
    }

    /**
     * Default implementation for toString.
     *
     * @return string representation
     */
    default String valueToString() {
        return getClass().getSimpleName() + "{}";
    }
}