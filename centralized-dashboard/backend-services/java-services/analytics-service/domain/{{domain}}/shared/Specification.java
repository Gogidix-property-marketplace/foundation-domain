package com.gogidix.dashboard.analytics.domain.UserManagement.shared;

import java.util.List;
import java.util.function.Predicate;

/**
 * Generic specification interface for implementing the Specification pattern.
 * Used to encapsulate business rules and combine them in a flexible way.
 *
 * @param <T> the type of object this specification applies to
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface Specification<T> extends Predicate<T> {

    /**
     * Checks if the candidate object satisfies this specification.
     *
     * @param candidate the object to test
     * @return true if the specification is satisfied, false otherwise
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * Creates a new specification that represents the logical AND of this and another specification.
     *
     * @param other the other specification to AND with
     * @return a new specification representing AND(this, other)
     */
    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    /**
     * Creates a new specification that represents the logical OR of this and another specification.
     *
     * @param other the other specification to OR with
     * @return a new specification representing OR(this, other)
     */
    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    /**
     * Creates a new specification that represents the logical NOT of this specification.
     *
     * @return a new specification representing NOT(this)
     */
    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }

    /**
     * Creates a specification that is always satisfied.
     *
     * @param <T> the type of object
     * @return a specification that always returns true
     */
    static <T> Specification<T> alwaysTrue() {
        return candidate -> true;
    }

    /**
     * Creates a specification that is never satisfied.
     *
     * @param <T> the type of object
     * @return a specification that always returns false
     */
    static <T> Specification<T> alwaysFalse() {
        return candidate -> false;
    }

    /**
     * Creates a specification from a predicate.
     *
     * @param predicate the predicate to wrap
     * @param <T>       the type of object
     * @return a specification that delegates to the predicate
     */
    static <T> Specification<T> from(Predicate<T> predicate) {
        return predicate::test;
    }

    /**
     * Creates a specification that combines multiple specifications with logical AND.
     *
     * @param specifications the specifications to combine
     * @param <T>            the type of object
     * @return a specification that returns true if all specifications are satisfied
     */
    @SafeVarargs
    static <T> Specification<T> allOf(Specification<T>... specifications) {
        return candidate -> {
            for (Specification<T> spec : specifications) {
                if (!spec.isSatisfiedBy(candidate)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Creates a specification that combines multiple specifications with logical OR.
     *
     * @param specifications the specifications to combine
     * @param <T>            the type of object
     * @return a specification that returns true if at least one specification is satisfied
     */
    @SafeVarargs
    static <T> Specification<T> anyOf(Specification<T>... specifications) {
        return candidate -> {
            for (Specification<T> spec : specifications) {
                if (spec.isSatisfiedBy(candidate)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Creates a specification that combines multiple specifications with logical OR from a list.
     *
     * @param specifications the list of specifications to combine
     * @param <T>            the type of object
     * @return a specification that returns true if at least one specification is satisfied
     */
    static <T> Specification<T> anyOf(List<Specification<T>> specifications) {
        return candidate -> specifications.stream().anyMatch(spec -> spec.isSatisfiedBy(candidate));
    }

    /**
     * Creates a specification that combines multiple specifications with logical AND from a list.
     *
     * @param specifications the list of specifications to combine
     * @param <T>            the type of object
     * @return a specification that returns true if all specifications are satisfied
     */
    static <T> Specification<T> allOf(List<Specification<T>> specifications) {
        return candidate -> specifications.stream().allMatch(spec -> spec.isSatisfiedBy(candidate));
    }

    @Override
    default boolean test(T t) {
        return isSatisfiedBy(t);
    }
}