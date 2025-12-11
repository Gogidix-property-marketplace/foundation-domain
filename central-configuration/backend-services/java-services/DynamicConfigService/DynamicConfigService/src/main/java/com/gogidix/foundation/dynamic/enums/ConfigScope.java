package com.gogidix.foundation.dynamic.enums;

public enum ConfigScope {
    GLOBAL("global"),
    APPLICATION("application"),
    SERVICE("service"),
    ENVIRONMENT("environment"),
    USER("user");

    private final String value;

    ConfigScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConfigScope fromValue(String value) {
        for (ConfigScope scope : ConfigScope.values()) {
            if (scope.getValue().equalsIgnoreCase(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Invalid ConfigScope value: " + value);
    }
}