package com.gogidix.ai.predictive.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Internationalization (i18n) configuration for EnterpriseTestService application.
 * Supports multiple languages and locale-aware message resolution.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class I18nConfig implements WebMvcConfigurer {

    private static final String BASENAME = "i18n/messages";
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),    // Spanish
            Locale.forLanguageTag("fr"),    // French
            Locale.forLanguageTag("de"),    // German
            Locale.forLanguageTag("it"),    // Italian
            Locale.forLanguageTag("pt"),    // Portuguese
            Locale.forLanguageTag("ru"),    // Russian
            Locale.forLanguageTag("ja"),    // Japanese
            Locale.forLanguageTag("zh"),    // Chinese
            Locale.forLanguageTag("ko"),    // Korean
            Locale.forLanguageTag("ar"),    // Arabic
            Locale.forLanguageTag("hi"),    // Hindi
            Locale.forLanguageTag("tr"),    // Turkish
            Locale.forLanguageTag("nl"),    // Dutch
            Locale.forLanguageTag("th"),    // Thai
            Locale.forLanguageTag("vi")     // Vietnamese
    );

    /**
     * Message source for internationalized messages.
     * Uses ReloadableResourceBundleMessageSource for hot-reloading in development.
     *
     * @return MessageSource
     */
    @Bean
    public MessageSource messageSource() {
        log.info("Configuring message source for i18n");

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(BASENAME);
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setCacheSeconds(60); // Cache for 1 minute in development
        messageSource.setFallbackToSystemLocale(true);

        log.debug("Supported locales: {}", SUPPORTED_LOCALES);
        log.debug("Message source basename: {}", BASENAME);

        return messageSource;
    }

    /**
     * Locale resolver for determining the current locale.
     * Uses AcceptHeaderLocaleResolver for header-based locale resolution.
     *
     * @return LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        log.info("Configuring locale resolver");

        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(SUPPORTED_LOCALES);

        return resolver;
    }

    /**
     * Session-based locale resolver (alternative implementation).
     *
     * @return LocaleResolver
     */
    @Bean
    @Profile("session-locale")
    public LocaleResolver sessionLocaleResolver() {
        log.info("Configuring session-based locale resolver");

        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);

        return resolver;
    }

    /**
     * Locale change interceptor for dynamic locale switching via request parameter.
     *
     * @return LocaleChangeInterceptor
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        log.info("Configuring locale change interceptor");

        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);

        return interceptor;
    }

    /**
     * Custom locale change interceptor with custom parameter name.
     *
     * @return LocaleChangeInterceptor
     */
    @Bean
    @Profile("custom-locale-param")
    public LocaleChangeInterceptor customLocaleChangeInterceptor() {
        log.info("Configuring custom locale change interceptor");

        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("locale");
        interceptor.setIgnoreInvalidLocale(true);

        return interceptor;
    }

    /**
     * Message source for validation messages.
     * Separate message source specifically for validation error messages.
     *
     * @return ResourceBundleMessageSource
     */
    @Bean
    public MessageSource validationMessageSource() {
        log.info("Configuring validation message source");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/validation");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setFallbackToSystemLocale(true);

        return messageSource;
    }

    /**
     * Message source for application-specific messages.
     * Separate message source for business logic messages.
     *
     * @return ResourceBundleMessageSource
     */
    @Bean
    public MessageSource applicationMessageSource() {
        log.info("Configuring application message source");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/application");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setFallbackToSystemLocale(true);

        return messageSource;
    }

    /**
     * Bean validation message source.
     * Configures validation messages to use i18n.
     *
     * @return LocalValidatorFactoryBean
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        log.info("Configuring bean validation with i18n support");

        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());

        return bean;
    }

    /**
     * Message source for exception messages.
     * Separate message source for exception and error messages.
     *
     * @return ResourceBundleMessageSource
     */
    @Bean
    public MessageSource exceptionMessageSource() {
        log.info("Configuring exception message source");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/exceptions");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setFallbackToSystemLocale(true);

        return messageSource;
    }

    /**
     * Adds the locale change interceptor to the registry.
     *
     * @param registry InterceptorRegistry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering locale change interceptor");
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * Custom locale resolver with fallback logic.
     *
     * @return CustomLocaleResolver
     */
    @Bean
    @Profile("custom-locale")
    public CustomLocaleResolver customLocaleResolver() {
        log.info("Configuring custom locale resolver with fallback logic");

        return new CustomLocaleResolver(SUPPORTED_LOCALES, Locale.ENGLISH);
    }

    /**
     * Message source provider for dependency injection.
     *
     * @return MessageSourceProvider
     */
    @Bean
    public MessageSourceProvider messageSourceProvider() {
        return new MessageSourceProvider(messageSource(), validationMessageSource(), exceptionMessageSource());
    }

    /**
     * Custom locale resolver implementation with advanced fallback logic.
     */
    public static class CustomLocaleResolver extends AcceptHeaderLocaleResolver {

        private final List<Locale> supportedLocales;
        private final Locale defaultLocale;

        public CustomLocaleResolver(List<Locale> supportedLocales, Locale defaultLocale) {
            this.supportedLocales = supportedLocales;
            this.defaultLocale = defaultLocale;
            this.setDefaultLocale(defaultLocale);
            this.setSupportedLocales(supportedLocales);
        }

        @Override
        public Locale resolveLocale(org.springframework.http.HttpServletRequest request) {
            // Try Accept-Language header first
            String acceptLanguage = request.getHeader("Accept-Language");
            if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                Locale requestedLocale = Locale.lookup(acceptLanguage, supportedLocales);
                if (requestedLocale != null) {
                    return requestedLocale;
                }
            }

            // Try query parameter
            String langParam = request.getParameter("lang");
            if (langParam != null && !langParam.isEmpty()) {
                Locale paramLocale = Locale.forLanguageTag(langParam);
                if (supportedLocales.contains(paramLocale)) {
                    return paramLocale;
                }
            }

            // Try session attribute
            Object sessionLocale = request.getSession().getAttribute("locale");
            if (sessionLocale instanceof Locale && supportedLocales.contains(sessionLocale)) {
                return (Locale) sessionLocale;
            }

            // Return default locale
            return defaultLocale;
        }
    }

    /**
     * Message source provider for convenient access to different message sources.
     */
    public static class MessageSourceProvider {
        private final MessageSource messageSource;
        private final MessageSource validationMessageSource;
        private final MessageSource exceptionMessageSource;

        public MessageSourceProvider(MessageSource messageSource, MessageSource validationMessageSource, MessageSource exceptionMessageSource) {
            this.messageSource = messageSource;
            this.validationMessageSource = validationMessageSource;
            this.exceptionMessageSource = exceptionMessageSource;
        }

        public MessageSource getMessageSource() { return messageSource; }
        public MessageSource getValidationMessageSource() { return validationMessageSource; }
        public MessageSource getExceptionMessageSource() { return exceptionMessageSource; }
    }
}