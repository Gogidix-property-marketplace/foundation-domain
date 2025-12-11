package com.gogidix.foundation.dynamic.enums;

public enum ChangeType {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    ACTIVATE("activate"),
    DEACTIVATE("deactivate");

    private final String value;

    ChangeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ChangeType fromValue(String value) {
        for (ChangeType type : ChangeType.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ChangeType value: " + value);
    }
}