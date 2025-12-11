package com.gogidix.foundation.config.enums;

public enum Environment {
    DEVELOPMENT("development"),
    STAGING("staging"),
    PRODUCTION("production"),
    TESTING("testing");

    private final String value;

    Environment(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Environment fromValue(String value) {
        for (Environment env : Environment.values()) {
            if (env.value.equalsIgnoreCase(value)) {
                return env;
            }
        }
        throw new IllegalArgumentException("Unknown environment: " + value);
    }
}