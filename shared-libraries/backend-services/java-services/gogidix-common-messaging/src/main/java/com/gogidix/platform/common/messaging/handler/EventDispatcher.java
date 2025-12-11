package com.gogidix.platform.common.messaging.handler;

import com.gogidix.platform.common.messaging.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dispatcher for routing events to appropriate handlers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final ListableBeanFactory beanFactory;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private final Map<Class<? extends DomainEvent>, List<EventHandler<?>>> handlerCache = new ConcurrentHashMap<>();

    /**
     * Dispatch an event to all registered handlers
     */
    public CompletableFuture<Void> dispatch(DomainEvent event) {
        log.debug("Dispatching event {} to handlers", event.getEventType());
        
        List<EventHandler<?>> handlers = getHandlersForEvent(event);
        
        if (handlers.isEmpty()) {
            log.warn("No handlers found for event type: {}", event.getEventType());
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (EventHandler<?> handler : handlers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.debug("Processing event {} with handler {}", event.getEventId(), handler.getClass().getSimpleName());
                    @SuppressWarnings("unchecked")
                    EventHandler<DomainEvent> typedHandler = (EventHandler<DomainEvent>) handler;
                    typedHandler.handle(event);
                    log.debug("Successfully processed event {} with handler {}", event.getEventId(), handler.getClass().getSimpleName());
                } catch (Exception ex) {
                    log.error("Error processing event {} with handler {}", 
                             event.getEventId(), handler.getClass().getSimpleName(), ex);
                    // Continue processing other handlers
                }
            }, executorService);
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Dispatch an event synchronously to all registered handlers
     */
    public void dispatchSync(DomainEvent event) {
        log.debug("Dispatching event {} synchronously to handlers", event.getEventType());
        
        List<EventHandler<?>> handlers = getHandlersForEvent(event);
        
        if (handlers.isEmpty()) {
            log.warn("No handlers found for event type: {}", event.getEventType());
            return;
        }

        for (EventHandler<?> handler : handlers) {
            try {
                log.debug("Processing event {} with handler {}", event.getEventId(), handler.getClass().getSimpleName());
                @SuppressWarnings("unchecked")
                EventHandler<DomainEvent> typedHandler = (EventHandler<DomainEvent>) handler;
                typedHandler.handle(event);
                log.debug("Successfully processed event {} with handler {}", event.getEventId(), handler.getClass().getSimpleName());
            } catch (Exception ex) {
                log.error("Error processing event {} with handler {}", 
                         event.getEventId(), handler.getClass().getSimpleName(), ex);
                // Continue processing other handlers
            }
        }
    }

    /**
     * Get all handlers for a specific event type
     */
    @SuppressWarnings("unchecked")
    private List<EventHandler<?>> getHandlersForEvent(DomainEvent event) {
        Class<? extends DomainEvent> eventClass = event.getClass();
        
        return handlerCache.computeIfAbsent(eventClass, clazz -> {
            Map<String, EventHandler> handlers = beanFactory.getBeansOfType(EventHandler.class);
            
            List<EventHandler<?>> filteredHandlers = new ArrayList<>();
            
            for (EventHandler handler : handlers.values()) {
                if (handler.canHandle(event)) {
                    filteredHandlers.add(handler);
                }
            }
            
            // Sort by priority (lower number = higher priority)
            filteredHandlers.sort(Comparator.comparingInt(EventHandler::getPriority));
            
            log.debug("Found {} handlers for event type {}", filteredHandlers.size(), clazz.getSimpleName());
            
            return filteredHandlers;
        });
    }

    /**
     * Clear the handler cache (useful for testing or when handlers are added/removed at runtime)
     */
    public void clearCache() {
        handlerCache.clear();
    }

    /**
     * Get the number of handlers for an event type
     */
    public int getHandlerCount(DomainEvent event) {
        return getHandlersForEvent(event).size();
    }
}
