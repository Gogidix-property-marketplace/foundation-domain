package com.gogidix.platform.common.messaging.handler;

import com.gogidix.platform.common.messaging.event.DomainEvent;

/**
 * Interface for handling domain events
 */
public interface EventHandler<T extends DomainEvent> {

    /**
     * Handle a domain event
     * @param event the domain event to handle
     * @throws Exception if handling fails
     */
    void handle(T event) throws Exception;

    /**
     * Get the event type this handler supports
     * @return the supported event class
     */
    Class<T> getEventType();

    /**
     * Get handler priority (lower number = higher priority)
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Check if this handler should process the given event
     * @param event the event to check
     * @return true if the handler should process the event
     */
    default boolean canHandle(DomainEvent event) {
        return getEventType().isInstance(event);
    }
}
