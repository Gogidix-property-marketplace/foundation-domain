package com.gogidix.dashboard.reporting.domain.UserManagement;

import com.gogidix.dashboard.reporting.domain.UserManagement.shared.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/**
 * Value Object representing the unique identifier for a Usermanagement.
 * Uses UUID for guaranteed uniqueness across distributed systems.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Getter
@EqualsAndHashCode
@Embeddable
public class Id implements ValueObject, Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id", columnDefinition = "UUID")
    private final UUID value;

    /**
     * Default constructor for JPA.
     */
    protected Id() {
        this.value = null;
    }

    /**
     * Private constructor.
     *
     * @param value the UUID value
     */
    private Id(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Id value cannot be null");
        }
        this.value = value;
    }

    /**
     * Creates a new Id with a random UUID.
     *
     * @return new Id instance
     */
    public static Id generate() {
        UUID uuid = UUID.randomUUID();
        log.trace("Generated new Id: {}", uuid);
        return new Id(uuid);
    }

    /**
     * Creates a Id from an existing UUID.
     *
     * @param uuid the UUID to wrap
     * @return new Id instance
     */
    public static Id of(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        return new Id(uuid);
    }

    /**
     * Creates a Id from a UUID string.
     *
     * @param uuidString the UUID string to parse
     * @return new Id instance
     */
    public static Id fromString(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            throw new IllegalArgumentException("UUID string cannot be null or empty");
        }

        try {
            UUID uuid = UUID.fromString(uuidString.trim());
            return new Id(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuidString, e);
        }
    }

    /**
     * Returns the string representation of the UUID.
     *
     * @return UUID as string
     */
    public String asString() {
        return value.toString();
    }

    /**
     * Returns the underlying UUID.
     *
     * @return the UUID value
     */
    public UUID asUuid() {
        return value;
    }

    /**
     * Compares this Id with another object.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        Id that = (Id) obj;
        return value.equals(that.value);
    }

    /**
     * Returns the hash code of the UUID.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Returns the string representation of this Id.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Id{" + value + "}";
    }

    /**
     * Checks if this Id is valid.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return value != null;
    }

    /**
     * Checks if this Id is empty (null value).
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * Creates a copy of this Id.
     *
     * @return new Id with the same value
     */
    public Id copy() {
        return value != null ? new Id(value) : null;
    }

    /**
     * Compares this Id with another based on the UUID natural ordering.
     *
     * @param other the other Id to compare with
     * @return -1, 0, or 1 as this Id is less than, equal to, or greater than the specified Id
     */
    public int compareTo(Id other) {
        if (other == null) {
            return 1;
        }
        if (this.value == null && other.value == null) {
            return 0;
        }
        if (this.value == null) {
            return -1;
        }
        if (other.value == null) {
            return 1;
        }
        return this.value.compareTo(other.value);
    }

    /**
     * Returns the timestamp component of the UUID (version 1 UUIDs only).
     * For version 4 UUIDs, returns 0.
     *
     * @return timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        if (value == null || value.version() != 1) {
            return 0L;
        }
        return value.timestamp();
    }

    /**
     * Returns the version of the UUID.
     *
     * @return UUID version number
     */
    public int getVersion() {
        return value != null ? value.version() : 0;
    }

    /**
     * Returns the variant of the UUID.
     *
     * @return UUID variant number
     */
    public int getVariant() {
        return value != null ? value.variant() : 0;
    }

    /**
     * Checks if this is a version 1 UUID (time-based).
     *
     * @return true if version 1, false otherwise
     */
    public boolean isVersion1() {
        return value != null && value.version() == 1;
    }

    /**
     * Checks if this is a version 4 UUID (random).
     *
     * @return true if version 4, false otherwise
     */
    public boolean isVersion4() {
        return value != null && value.version() == 4;
    }
}