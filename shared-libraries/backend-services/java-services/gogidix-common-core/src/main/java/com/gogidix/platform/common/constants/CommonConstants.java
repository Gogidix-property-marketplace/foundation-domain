package com.gogidix.platform.common.constants;

/**
 * Common constants used across all Gogidix platform services
 */
public final class CommonConstants {

    // Prevent instantiation
    private CommonConstants() {}

    // HTTP Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_X_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_X_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_X_USER_ID = "X-User-ID";
    public static final String HEADER_X_TENANT_ID = "X-Tenant-ID";
    public static final String HEADER_X_API_KEY = "X-API-Key";
    public static final String HEADER_X_VERSION = "X-Version";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_PRAGMA = "Pragma";
    public static final String HEADER_EXPIRES = "Expires";
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

    // Content Types
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART_FORM = "multipart/form-data";
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_CSV = "text/csv";
    public static final String CONTENT_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // Character Sets
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String CHARSET_ISO_8859_1 = "ISO-8859-1";

    // Date Formats
    public static final String DATE_FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String DATE_FORMAT_ISO_DATE = "yyyy-MM-dd";
    public static final String DATE_FORMAT_ISO_TIME = "HH:mm:ss";
    public static final String DATE_FORMAT_SLASH_DATE = "MM/dd/yyyy";
    public static final String DATE_FORMAT_EUROPEAN_DATE = "dd-MM-yyyy";
    public static final String DATE_FORMAT_READABLE = "MMMM dd, yyyy";
    public static final String DATE_FORMAT_READABLE_DATETIME = "MMMM dd, yyyy HH:mm a";

    // Time Zones
    public static final String TIMEZONE_UTC = "UTC";
    public static final String TIMEZONE_EST = "America/New_York";
    public static final String TIMEZONE_PST = "America/Los_Angeles";
    public static final String TIMEZONE_LONDON = "Europe/London";
    public static final String TIMEZONE_TOKYO = "Asia/Tokyo";

    // Pagination
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_SORT = "sort";
    public static final String PARAM_DIRECTION = "direction";
    public static final String PARAM_FILTER = "filter";
    public static final String PARAM_SEARCH = "search";
    public static final String PARAM_OFFSET = "offset";
    public static final String PARAM_LIMIT = "limit";

    // Default Values
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 1000;
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long DEFAULT_CACHE_TTL = 3600; // 1 hour in seconds

    // HTTP Methods
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_PUT = "PUT";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_PATCH = "PATCH";
    public static final String HTTP_HEAD = "HEAD";
    public static final String HTTP_OPTIONS = "OPTIONS";

    // HTTP Status Codes
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;

    // Error Codes
    public static final String ERROR_GENERIC = "GENERIC_ERROR";
    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    public static final String ERROR_BUSINESS = "BUSINESS_ERROR";
    public static final String ERROR_RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ERROR_FORBIDDEN = "FORBIDDEN";
    public static final String ERROR_CONFLICT = "CONFLICT";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String ERROR_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String ERROR_TIMEOUT = "TIMEOUT";
    public static final String ERROR_DEPENDENCY_FAILURE = "DEPENDENCY_FAILURE";

    // Security
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BASIC_PREFIX = "Basic ";
    public static final String TOKEN_TYPE_BEARER = "bearer";
    public static final String TOKEN_TYPE_JWT = "jwt";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TENANT_ID = "tenantId";

    // Cache Keys
    public static final String CACHE_KEY_PREFIX = "gogidix:";
    public static final String CACHE_USER_PREFIX = "user:";
    public static final String CACHE_SESSION_PREFIX = "session:";
    public static final String CACHE_CONFIG_PREFIX = "config:";
    public static final String CACHE_PERMISSIONS_PREFIX = "permissions:";
    public static final String CACHE_RATE_LIMIT_PREFIX = "rate_limit:";

    // Regex Patterns
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PHONE_REGEX = "^[+]?[1-9]\\d{1,14}$";
    public static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";
    public static final String SLUG_REGEX = "^[a-z0-9-]+$";

    // File Extensions
    public static final String EXTENSION_JSON = ".json";
    public static final String EXTENSION_XML = ".xml";
    public static final String EXTENSION_PDF = ".pdf";
    public static final String EXTENSION_CSV = ".csv";
    public static final String EXTENSION_XLSX = ".xlsx";
    public static final String EXTENSION_JPG = ".jpg";
    public static final String EXTENSION_JPEG = ".jpeg";
    public static final String EXTENSION_PNG = ".png";
    public static final String EXTENSION_GIF = ".gif";
    public static final String EXTENSION_TXT = ".txt";
    public static final String EXTENSION_ZIP = ".zip";

    // Environment
    public static final String ENV_DEV = "dev";
    public static final String ENV_DEVELOPMENT = "development";
    public static final String ENV_TEST = "test";
    public static final String ENV_STAGING = "staging";
    public static final String ENV_PROD = "prod";
    public static final String ENV_PRODUCTION = "production";

    // Spring Profiles
    public static final String PROFILE_DEFAULT = "default";
    public static final String PROFILE_LOCAL = "local";
    public static final String PROFILE_CLOUD = "cloud";
    public static final String PROFILE_KUBERNETES = "kubernetes";
    public static final String PROFILE_DOCKER = "docker";

    // Metrics
    public static final String METRIC_PREFIX = "gogidix.";
    public static final String METRIC_HTTP_REQUESTS = METRIC_PREFIX + "http.requests";
    public static final String METRIC_HTTP_RESPONSE_TIME = METRIC_PREFIX + "http.response_time";
    public static final String METRIC_HTTP_ERRORS = METRIC_PREFIX + "http.errors";
    public static final String METRIC_DB_CONNECTIONS = METRIC_PREFIX + "db.connections";
    public static final String METRIC_CACHE_HITS = METRIC_PREFIX + "cache.hits";
    public static final String METRIC_CACHE_MISSES = METRIC_PREFIX + "cache.misses";

    // Logging
    public static final String LOG_REQUEST_ID = "requestId";
    public static final String LOG_CORRELATION_ID = "correlationId";
    public static final String LOG_USER_ID = "userId";
    public static final String LOG_TENANT_ID = "tenantId";
    public static final String LOG_SERVICE_NAME = "serviceName";
    public static final String LOG_DURATION = "duration";
    public static final String LOG_STATUS_CODE = "statusCode";
    public static final String LOG_ERROR_MESSAGE = "errorMessage";

    // Application Properties
    public static final String SPRING_APP_NAME = "spring.application.name";
    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    public static final String SERVER_PORT = "server.port";
    public static final String MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE = "management.endpoints.web.exposure.include";

    // Business Constants
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_FILE_SIZE_MB = 10;
    public static final int MAX_ATTEMPTS_LOGIN = 5;
    public static final int LOCK_TIME_MINUTES = 30;
    public static final int TOKEN_EXPIRE_MINUTES = 60;
    public static final int REFRESH_TOKEN_EXPIRE_DAYS = 7;

    // Currency
    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_EUR = "EUR";
    public static final String CURRENCY_GBP = "GBP";
    public static final String CURRENCY_JPY = "JPY";
    public static final String CURRENCY_DEFAULT = CURRENCY_USD;

    // Language
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_SPANISH = "es";
    public static final String LANG_FRENCH = "fr";
    public static final String LANG_GERMAN = "de";
    public static final String LANG_JAPANESE = "ja";
    public static final String LANG_DEFAULT = LANG_ENGLISH;

    // API Version
    public static final String API_V1 = "v1";
    public static final String API_V2 = "v2";
    public static final String API_CURRENT = API_V1;
}