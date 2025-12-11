package com.gogidix.ai.personalization.domain.UserManagement.policy;

import com.gogidix.ai.personalization.domain.UserManagement.;
import com.gogidix.ai.personalization.domain.UserManagement.Status;
import com.gogidix.ai.personalization.domain.UserManagement.shared.Specification;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Business policy for  operations.
 * Encapsulates business rules and constraints for Usermanagement management.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
public class BusinessPolicy {

    /**
     * Checks if a Usermanagement can be activated.
     *
     * @param UserManagement the Usermanagement to check
     * @return true if activation is allowed
     */
    public static boolean canActivate( UserManagement) {
        log.debug("Checking if Usermanagement {} can be activated", UserManagement.getId());

        // Business rules for activation
        boolean canActivate = UserManagement.getStatus() == Status.INACTIVE ||
                UserManagement.getStatus() == Status.SUSPENDED;

        log.debug(" {} {} be activated", UserManagement.getId(), canActivate ? "can" : "cannot");
        return canActivate;
    }

    /**
     * Checks if a Usermanagement can be deactivated.
     *
     * @param UserManagement the Usermanagement to check
     * @return true if deactivation is allowed
     */
    public static boolean canDeactivate( UserManagement) {
        log.debug("Checking if Usermanagement {} can be deactivated", UserManagement.getId());

        // Business rules for deactivation
        boolean canDeactivate = UserManagement.getStatus() == Status.ACTIVE;

        log.debug(" {} {} be deactivated", UserManagement.getId(), canDeactivate ? "can" : "cannot");
        return canDeactivate;
    }

    /**
     * Checks if a Usermanagement can be archived.
     *
     * @param UserManagement the Usermanagement to check
     * @return true if archiving is allowed
     */
    public static boolean canArchive( UserManagement) {
        log.debug("Checking if Usermanagement {} can be archived", UserManagement.getId());

        // Business rules for archiving
        boolean canArchive = UserManagement.getStatus() == Status.INACTIVE ||
                UserManagement.getStatus() == Status.TERMINATED;

        // Additional business rule: must be inactive for at least 30 days
        if (canArchive && UserManagement.getStatus() == Status.INACTIVE) {
            long daysInactive = ChronoUnit.DAYS.between(UserManagement.getUpdatedAt(), LocalDateTime.now());
            canArchive = daysInactive >= 30;
        }

        log.debug(" {} {} be archived", UserManagement.getId(), canArchive ? "can" : "cannot");
        return canArchive;
    }

    /**
     * Checks if a Usermanagement can be deleted.
     *
     * @param UserManagement the Usermanagement to check
     * @return true if deletion is allowed
     */
    public static boolean canDelete( UserManagement) {
        log.debug("Checking if Usermanagement {} can be deleted", UserManagement.getId());

        // Business rules for deletion
        boolean canDelete = UserManagement.getStatus() == Status.INACTIVE ||
                UserManagement.getStatus() == Status.ARCHIVED;

        // Additional business rule: must be inactive for at least 90 days
        if (canDelete) {
            long daysInactive = ChronoUnit.DAYS.between(UserManagement.getUpdatedAt(), LocalDateTime.now());
            canDelete = daysInactive >= 90;
        }

        log.debug(" {} {} be deleted", UserManagement.getId(), canDelete ? "can" : "cannot");
        return canDelete;
    }

    /**
     * Checks if a Usermanagement can be updated.
     *
     * @param UserManagement the Usermanagement to check
     * @return true if updates are allowed
     */
    public static boolean canUpdate( UserManagement) {
        log.debug("Checking if Usermanagement {} can be updated", UserManagement.getId());

        // Business rules for updates
        boolean canUpdate = UserManagement.getStatus() != Status.ARCHIVED &&
                UserManagement.getStatus() != Status.TERMINATED &&
                UserManagement.getStatus() != Status.PENDING_DELETION;

        log.debug(" {} {} be updated", UserManagement.getId(), canUpdate ? "can" : "cannot");
        return canUpdate;
    }

    /**
     * Validates Usermanagement name according to business rules.
     *
     * @param name the name to validate
     * @return true if name is valid
     */
    public static boolean isValidName(String name) {
        log.debug("Validating Usermanagement name: {}", name);

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Business rules for name validation
        boolean isValid = name.length() >= 2 &&
                name.length() <= 100 &&
                name.matches("^[a-zA-Z0-9\\s\\-_]+$") &&
                !name.toLowerCase().contains("admin") &&
                !name.toLowerCase().contains("system") &&
                !name.toLowerCase().contains("root");

        log.debug(" name '{}' is {}", name, isValid ? "valid" : "invalid");
        return isValid;
    }

    /**
     * Creates a specification for active Usermanagement entities.
     *
     * @return specification for active Usermanagement entities
     */
    public static Specification<> activeSpecification() {
        return new ActiveSpecification();
    }

    /**
     * Creates a specification for Usermanagement entities created within date range.
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return specification for date range
     */
    public static Specification<> createdBetweenSpecification(LocalDateTime startDate, LocalDateTime endDate) {
        return new CreatedBetweenSpecification(startDate, endDate);
    }

    /**
     * Creates a specification for Usermanagement entities with name containing text.
     *
     * @param searchText the search text
     * @return specification for name search
     */
    public static Specification<> nameContainsSpecification(String searchText) {
        return new NameContainsSpecification(searchText);
    }

    /**
     * Specification for active Usermanagement entities.
     */
    private static class ActiveSpecification implements Specification<> {
        @Override
        public boolean isSatisfiedBy( UserManagement) {
            return UserManagement.getStatus() == Status.ACTIVE;
        }
    }

    /**
     * Specification for Usermanagement entities created within date range.
     */
    private static class CreatedBetweenSpecification implements Specification<> {
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public CreatedBetweenSpecification(LocalDateTime startDate, LocalDateTime endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public boolean isSatisfiedBy( UserManagement) {
            return !UserManagement.getCreatedAt().isBefore(startDate) && !UserManagement.getCreatedAt().isAfter(endDate);
        }
    }

    /**
     * Specification for Usermanagement entities with name containing text.
     */
    private static class NameContainsSpecification implements Specification<> {
        private final String searchText;

        public NameContainsSpecification(String searchText) {
            this.searchText = searchText.toLowerCase();
        }

        @Override
        public boolean isSatisfiedBy( UserManagement) {
            return UserManagement.getName().getValue().toLowerCase().contains(searchText);
        }
    }
}