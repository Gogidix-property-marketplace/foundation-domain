package com.gogidix.infrastructure.waf;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WafRuleEngine {
    private final List<WafRule> rules;
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public WafRuleEngine() {
        this.rules = List.of();
    }

    public WafAction evaluateRequest(String request) {
        requestCounter.incrementAndGet();

        // Simple evaluation logic
        for (WafRule rule : rules) {
            if (shouldApplyRule(rule, request)) {
                return rule.getAction();
            }
        }

        return WafAction.ALLOW;
    }

    private boolean shouldApplyRule(WafRule rule, String request) {
        // Simplified rule matching
        return true;
    }

    public int getRequestCount() {
        return requestCounter.get();
    }
}
