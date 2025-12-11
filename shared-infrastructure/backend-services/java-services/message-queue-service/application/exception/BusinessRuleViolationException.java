package com.gogidix.infrastructure.queue.application.exception;

/**
 * Exception thrown when business rules are violated.
 * Represents business rule violations in the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class BusinessRuleViolationException extends ApplicationException {

    private final String ruleName;
    private final Object ruleParameters;

    /**
     * Constructs a new BusinessRuleViolationException with the specified message.
     *
     * @param message the error message
     */
    public BusinessRuleViolationException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
        this.ruleName = null;
        this.ruleParameters = null;
    }

    /**
     * Constructs a new BusinessRuleViolationException for a specific rule.
     *
     * @param message        the error message
     * @param ruleName       the name of the violated rule
     * @param ruleParameters the parameters of the violated rule
     */
    public BusinessRuleViolationException(String message, String ruleName, Object ruleParameters) {
        super(message, "BUSINESS_RULE_VIOLATION");
        this.ruleName = ruleName;
        this.ruleParameters = ruleParameters;
    }

    /**
     * Gets the name of the violated business rule.
     *
     * @return the rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Gets the parameters of the violated business rule.
     *
     * @return the rule parameters
     */
    public Object getRuleParameters() {
        return ruleParameters;
    }
}