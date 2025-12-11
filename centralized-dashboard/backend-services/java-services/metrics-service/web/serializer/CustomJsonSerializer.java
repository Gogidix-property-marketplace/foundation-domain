package com.gogidix.dashboard.metrics.web.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom JSON serializer for EnterpriseTestService-EnterpriseTestService.
 * Provides custom serialization rules for JSON responses.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class CustomJsonSerializer {

    /**
     * Creates a configured ObjectMapper with custom serializers.
     *
     * @return configured ObjectMapper
     */
    public static ObjectMapper createCustomObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register custom serializers
        SimpleModule module = new SimpleModule("CustomSerializers");

        // Custom LocalDateTime serializer
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());

        mapper.registerModule(module);

        return mapper;
    }

    /**
     * Custom LocalDateTime serializer.
     */
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public void serialize(LocalDateTime dateTime, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(dateTime.format(formatter));
        }
    }

    /**
     * Custom null serializer for consistent null handling.
     */
    public static class NullSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNull();
        }
    }

    /**
     * Custom boolean serializer for lowercase boolean values.
     */
    public static class LowercaseBooleanSerializer extends JsonSerializer<Boolean> {
        @Override
        public void serialize(Boolean value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value ? "true" : "false");
        }
    }

    /**
     * Custom number serializer for consistent number formatting.
     */
    public static class PreciseNumberSerializer extends JsonSerializer<Number> {
        @Override
        public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value instanceof Double || value instanceof Float) {
                gen.writeString(String.format("%.2f", value.doubleValue()));
            } else {
                gen.writeString(value.toString());
            }
        }
    }
}