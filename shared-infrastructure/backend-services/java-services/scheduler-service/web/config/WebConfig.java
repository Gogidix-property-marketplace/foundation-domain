package com.gogidix.infrastructure.scheduler.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Web configuration for EnterpriseTestService application.
 * Configures message converters, content negotiation, and other web settings.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure message converters for JSON serialization.
     *
     * @param converters list of HTTP message converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        // Configure object mapper
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        converter.setObjectMapper(objectMapper);

        // Supported media types
        converter.setSupportedMediaTypes(List.of(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON_UTF8,
            new MediaType("application", "hal+json")
        ));

        converters.add(converter);
    }

    /**
     * Configure content negotiation.
     *
     * @param configurer content negotiation configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(true)
            .parameterName("format")
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML)
            .mediaType("hal", new MediaType("application", "hal+json"));
    }

    /**
     * Configure CORS settings.
     *
     * @param registry CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:4200", "https://EnterpriseTestService.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .exposedHeaders("X-Total-Count", "X-Page-Count")
            .allowCredentials(true)
            .maxAge(3600);
    }

    /**
     * Configure formatters for date handling.
     *
     * @param registry formatter registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // LocalDateTime formatter
        registry.addFormatterForFieldType(LocalDateTime.class, new org.springframework.format.Formatter<LocalDateTime>() {
            @Override
            public LocalDateTime parse(String text, java.util.Locale locale) throws java.text.ParseException {
                try {
                    return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    throw new java.text.ParseException("Invalid date format: " + text, 0);
                }
            }

            @Override
            public String print(LocalDateTime object, java.util.Locale locale) {
                return object.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });

        // Enum formatter for status values
        registry.addConverter(new org.springframework.core.convert.converter.Converter<String, com.gogidix.UserManagement.EnterpriseTestService.domain.UserManagement.Status>() {
            @Override
            public com.gogidix.UserManagement.EnterpriseTestService.domain.UserManagement.Status convert(String source) {
                return com.gogidix.UserManagement.EnterpriseTestService.domain.UserManagement.Status.valueOf(source.toUpperCase());
            }
        });
    }

    /**
     * Configure static resource handling.
     *
     * @param registry resource handler registry
     */
    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/");

        registry.addResourceHandler("/api-docs/**")
            .addResourceLocations("classpath:/META-INF/resources/");
    }
}