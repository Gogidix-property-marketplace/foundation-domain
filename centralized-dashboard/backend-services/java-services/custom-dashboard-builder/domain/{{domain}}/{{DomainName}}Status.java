package com.gogidix.dashboard.builder.domain.UserManagement;

/**
 * Enumeration representing the status of a Usermanagement.
 * Defines the lifecycle states that a Usermanagement can be in.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public enum Status {

    /**
     * The Usermanagement is active and fully functional.
     * This is the normal operational state.
     */
    ACTIVE("ACTIVE", "The Usermanagement is active and fully operational"),

    /**
     * The Usermanagement is temporarily inactive.
     * This state is used when the Usermanagement is temporarily disabled but can be reactivated.
     */
    INACTIVE("INACTIVE", "The Usermanagement is temporarily inactive"),

    /**
     * The Usermanagement has been archived and is no longer in use.
     * This state is used for historical purposes and the Usermanagement cannot be reactivated.
     */
    ARCHIVED("ARCHIVED", "The Usermanagement has been archived and is no longer active"),

    /**
     * The Usermanagement is pending activation or approval.
     * This is a transitional state before becoming active.
     */
    PENDING("PENDING", "The Usermanagement is pending activation"),

    /**
     * The Usermanagement is suspended due to policy violations or issues.
     * This is a temporary state that requires manual intervention.
     */
    SUSPENDED("SUSPENDED", "The Usermanagement has been suspended due to violations"),

    /**
     * The Usermanagement is under review.
     * This state is used when the Usermanagement is being reviewed by administrators.
     */
    UNDER_REVIEW("UNDER_REVIEW", "The Usermanagement is currently under review"),

    /**
     * The Usermanagement is being deleted or is marked for deletion.
     * This is a transitional state before permanent removal.
     */
    PENDING_DELETION("PENDING_DELETION", "The Usermanagement is marked for deletion"),

    /**
     * The Usermanagement has been terminated and is no longer accessible.
     * This is a final state that cannot be reversed.
     */
    TERMINATED("TERMINATED", "The Usermanagement has been terminated");

    private final String code;
    private final String description;

    /**
     * Constructor for Status enum.
     *
     * @param code        the status code
     * @param description the status description
     */
    Status(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the status code.
     *
     * @return the status code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the status description.
     *
     * @return the status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Finds a status by its code.
     *
     * @param code the status code to find
     * @return the corresponding status
     * @throws IllegalArgumentException if the code is not found
     */
    public static Status fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Status code cannot be null or empty");
        }

        for (Status status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("No Status found for code: " + code);
    }

    /**
     * Checks if the status is a terminal state.
     *
     * @return true if the status is terminal, false otherwise
     */
    public boolean isTerminal() {
        return this == ARCHIVED || this == TERMINATED;
    }

    /**
     * Checks if the status is active or can become active.
     *
     * @return true if the status allows activation, false otherwise
     */
    public boolean canActivate() {
        return this == INACTIVE || this == PENDING || this == SUSPENDED || this == UNDER_REVIEW;
    }

    /**
     * Checks if the status is transitional.
     *
     * @return true if the status is transitional, false otherwise
     */
    public boolean isTransitional() {
        return this == PENDING || this == PENDING_DELETION || this == UNDER_REVIEW;
    }

    /**
     * Checks if the status allows modification.
     *
     * @return true if modification is allowed, false otherwise
     */
    public boolean allowsModification() {
        return this == ACTIVE || this == INACTIVE || this == PENDING || this == UNDER_REVIEW;
    }

    /**
     * Checks if the status allows deletion.
     *
     * @return true if deletion is allowed, false otherwise
     */
    public boolean allowsDeletion() {
        return this == INACTIVE || this == PENDING_DELETION;
    }

    /**
     * Checks if the status requires approval.
     *
     * @return true if approval is required, false otherwise
     */
    public boolean requiresApproval() {
        return this == PENDING || this == UNDER_REVIEW || this == SUSPENDED;
    }

    /**
     * Gets the next logical status after this one.
     *
     * @return the next logical status
     */
    public Status getNextLogicalStatus() {
        switch (this) {
            case PENDING:
                return ACTIVE;
            case UNDER_REVIEW:
                return ACTIVE;
            case SUSPENDED:
                return INACTIVE;
            case PENDING_DELETION:
                return ARCHIVED;
            default:
                return this; // No transition
        }
    }

    /**
     * Gets all possible transitions from this status.
     *
     * @return array of possible next statuses
     */
    public Status[] getPossibleTransitions() {
        switch (this) {
            case PENDING:
                return new Status[]{ACTIVE, UNDER_REVIEW, PENDING_DELETION};
            case UNDER_REVIEW:
                return new Status[]{ACTIVE, SUSPENDED, PENDING_DELETION};
            case ACTIVE:
                return new Status[]{INACTIVE, SUSPENDED, PENDING_DELETION, UNDER_REVIEW};
            case INACTIVE:
                return new Status[]{ACTIVE, ARCHIVED, PENDING_DELETION};
            case SUSPENDED:
                return new Status[]{ACTIVE, INACTIVE, PENDING_DELETION, UNDER_REVIEW};
            case PENDING_DELETION:
                return new Status[]{ARCHIVED, TERMINATED};
            default:
                return new Status[]{};
        }
    }

    /**
     * Checks if a transition to the target status is allowed.
     *
     * @param targetStatus the target status
     * @return true if transition is allowed, false otherwise
     */
    public boolean canTransitionTo(Status targetStatus) {
        if (targetStatus == null || targetStatus == this) {
            return false;
        }

        for (Status possibleTransition : getPossibleTransitions()) {
            if (possibleTransition == targetStatus) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the user-friendly display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
    }

    /**
     * Returns all active statuses.
     *
     * @return array of active statuses
     */
    public static Status[] getActiveStatuses() {
        return new Status[]{ACTIVE, PENDING, UNDER_REVIEW};
    }

    /**
     * Returns all inactive statuses.
     *
     * @return array of inactive statuses
     */
    public static Status[] getInactiveStatuses() {
        return new Status[]{INACTIVE, SUSPENDED, PENDING_DELETION};
    }

    /**
     * Returns all terminal statuses.
     *
     * @return array of terminal statuses
     */
    public static Status[] getTerminalStatuses() {
        return new Status[]{ARCHIVED, TERMINATED};
    }

    @Override
    public String toString() {
        return String.format("%s (%s): %s", name(), code, description);
    }
}