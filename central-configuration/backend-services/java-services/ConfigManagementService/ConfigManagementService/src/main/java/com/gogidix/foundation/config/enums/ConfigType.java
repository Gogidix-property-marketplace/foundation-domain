package com.gogidix.foundation.config.enums;

public enum ConfigType {
    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    DOUBLE("double"),
    JSON("json"),
    YAML("yaml"),
    ENCRYPTED("encrypted");

    private final String value;

    ConfigType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConfigType fromValue(String value) {
        for (ConfigType type : ConfigType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown config type: " + value);
    }
}