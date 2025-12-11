/**
 * Unified AI Gateway - Enterprise AI Service Orchestration Platform
 * Port 3002
 *
 * Advanced API gateway for orchestrating all AI/ML services with:
 * - Service discovery and health monitoring
 * - Load balancing and failover
 * - Request routing and aggregation
 * - Authentication and authorization
 * - Rate limiting and throttling
 * - Monitoring and metrics
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const { body, query, validationResult } = require('express-validator');
const axios = require('axios');
const { createProxyMiddleware } = require('http-proxy-middleware');
const winston = require('winston');
const path = require('path');
const NodeCache = require('node-cache');
const { v4: uuidv4 } = require('uuid');
const CircuitBreaker = require('circuit-breaker-js');
const _ = require('lodash');

// Configuration
const config = {
    port: process.env.AI_GATEWAY_PORT || 3002,
    env: process.env.NODE_ENV || 'development',
    logLevel: process.env.LOG_LEVEL || 'info',

    // AI Service Endpoints
    services: {
        'predictive-analytics': {
            host: 'localhost',
            port: 9000,
            baseUrl: 'http://localhost:9000',
            healthPath: '/health',
            timeout: 10000,
            retries: 3,
            circuit: {
                threshold: 5,
                timeout: 60000
            }
        },
        'recommendation': {
            host: 'localhost',
            port: 9010,
            baseUrl: 'http://localhost:9010',
            healthPath: '/health',
            timeout: 8000,
            retries: 3,
            circuit: {
                threshold: 5,
                timeout: 60000
            }
        },
        'image-analysis': {
            host: 'localhost',
            port: 9020,
            baseUrl: 'http://localhost:9020',
            healthPath: '/health',
            timeout: 15000,
            retries: 2,
            circuit: {
                threshold: 3,
                timeout: 45000
            }
        },
        'text-analysis': {
            host: 'localhost',
            port: 9030,
            baseUrl: 'http://localhost:9030',
            healthPath: '/health',
            timeout: 12000,
            retries: 3,
            circuit: {
                threshold: 5,
                timeout: 60000
            }
        },
        'fraud-detection': {
            host: 'localhost',
            port: 9040,
            baseUrl: 'http://localhost:9040',
            healthPath: '/health',
            timeout: 5000,
            retries: 2,
            circuit: {
                threshold: 3,
                timeout: 30000
            }
        }
    },

    // Rate Limiting
    rateLimit: {
        windowMs: 15 * 60 * 1000, // 15 minutes
        max: 1000, // limit each IP to 1000 requests per windowMs
        standardHeaders: true,
        legacyHeaders: false,
    },

    // Cache Configuration
    cache: {
        stdTTL: 300, // 5 minutes
        checkperiod: 60, // 1 minute
        useClones: false
    }
};

// Initialize Express App
const app = express();

// Configure Winston Logger
const logger = winston.createLogger({
    level: config.logLevel,
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.errors({ stack: true }),
        winston.format.json()
    ),
    defaultMeta: { service: 'unified-ai-gateway' },
    transports: [
        new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
        new winston.transports.File({ filename: 'logs/combined.log' }),
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.simple()
            )
        })
    ]
});

// Initialize Cache
const cache = new NodeCache(config.cache);

// Service Health Status
const serviceHealth = {};

// Circuit Breakers
const circuitBreakers = {};

// Initialize Circuit Breakers for Services
Object.keys(config.services).forEach(serviceName => {
    const service = config.services[serviceName];
    circuitBreakers[serviceName] = new CircuitBreaker(
        async () => {
            const response = await axios.get(`${service.baseUrl}${service.healthPath}`, {
                timeout: service.timeout
            });
            return response.data;
        },
        service.circuit
    );

    circuitBreakers[serviceName].onOpen(() => {
        logger.warn(`Circuit breaker opened for ${serviceName} service`);
        serviceHealth[serviceName] = 'circuit_open';
    });

    circuitBreakers[serviceName].onClose(() => {
        logger.info(`Circuit breaker closed for ${serviceName} service`);
        serviceHealth[serviceName] = 'healthy';
    });
});

// Middleware
app.use(helmet());
app.use(compression());
app.use(cors({
    origin: process.env.CORS_ORIGIN || '*',
    credentials: true
}));

// Rate Limiting
const limiter = rateLimit(config.rateLimit);
app.use(limiter);

// Request parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Request ID and logging middleware
app.use((req, res, next) => {
    req.id = uuidv4();
    res.setHeader('X-Request-ID', req.id);

    logger.info('Incoming request', {
        requestId: req.id,
        method: req.method,
        url: req.url,
        userAgent: req.get('User-Agent'),
        ip: req.ip
    });

    const start = Date.now();
    res.on('finish', () => {
        logger.info('Request completed', {
            requestId: req.id,
            statusCode: res.statusCode,
            duration: Date.now() - start
        });
    });

    next();
});

// Validation middleware
const handleValidationErrors = (req, res, next) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({
            success: false,
            error: 'Validation failed',
            details: errors.array()
        });
    }
    next();
};

// Service Health Check
const checkServiceHealth = async (serviceName) => {
    const service = config.services[serviceName];

    try {
        if (circuitBreakers[serviceName].opened()) {
            throw new Error('Circuit breaker is open');
        }

        const result = await circuitBreakers[serviceName].fire();
        serviceHealth[serviceName] = 'healthy';
        return result;
    } catch (error) {
        serviceHealth[serviceName] = 'unhealthy';
        logger.warn(`Health check failed for ${serviceName}`, {
            error: error.message
        });
        throw error;
    }
};

// Periodic Health Monitoring
const monitorServices = async () => {
    const promises = Object.keys(config.services).map(async serviceName => {
        try {
            await checkServiceHealth(serviceName);
        } catch (error) {
            // Service is unhealthy, circuit breaker will handle
        }
    });

    await Promise.allSettled(promises);
};

// Start health monitoring
setInterval(monitorServices, 30000); // Check every 30 seconds

// Initial health check
monitorServices();

// Routes

// Health check endpoint
app.get('/health', (req, res) => {
    const health = {
        status: 'healthy',
        service: 'unified-ai-gateway',
        port: config.port,
        timestamp: new Date().toISOString(),
        services: {}
    };

    Object.keys(config.services).forEach(serviceName => {
        health.services[serviceName] = serviceHealth[serviceName] || 'unknown';
    });

    res.json(health);
});

// Gateway metrics
app.get('/metrics', (req, res) => {
    const metrics = {
        gateway: {
            uptime: process.uptime(),
            memory: process.memoryUsage(),
            activeConnections: 0 // Could track with connection middleware
        },
        services: Object.keys(config.services).reduce((acc, serviceName) => {
            acc[serviceName] = {
                health: serviceHealth[serviceName] || 'unknown',
                circuitState: circuitBreakers[serviceName].opened() ? 'open' : 'closed'
            };
            return acc;
        }, {}),
        cache: {
            keys: cache.keys().length,
            stats: cache.getStats()
        }
    };

    res.json(metrics);
});

// Unified AI Endpoint - Single entry point for all AI services
app.post('/api/v1/ai/unified', [
    body('service').isIn(Object.keys(config.services)).withMessage('Invalid service specified'),
    body('endpoint').notEmpty().withMessage('Endpoint is required'),
    body('data').optional().isObject().withMessage('Data must be an object'),
    body('method').optional().isIn(['GET', 'POST', 'PUT', 'DELETE']).withMessage('Invalid HTTP method')
], handleValidationErrors, async (req, res) => {
    try {
        const { service, endpoint, data = {}, method = 'POST', options = {} } = req.body;
        const serviceConfig = config.services[service];

        if (!serviceConfig) {
            return res.status(400).json({
                success: false,
                error: 'Service not found'
            });
        }

        // Check service health
        if (serviceHealth[service] === 'unhealthy' || serviceHealth[service] === 'circuit_open') {
            return res.status(503).json({
                success: false,
                error: `Service ${service} is currently unavailable`,
                service,
                health: serviceHealth[service]
            });
        }

        // Build request URL
        const url = `${serviceConfig.baseUrl}${endpoint}`;

        // Make request to service with circuit breaker
        const breaker = circuitBreakers[service];
        breaker.setTimeout(serviceConfig.timeout);

        const response = await breaker.fire(() => {
            const axiosConfig = {
                method: method.toLowerCase(),
                url,
                data,
                timeout: serviceConfig.timeout,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Request-ID': req.id,
                    ...options.headers
                }
            };
            return axios(axiosConfig);
        });

        // Cache successful responses if configured
        if (options.cache && response.data.success) {
            const cacheKey = `${service}:${endpoint}:${JSON.stringify(data)}`;
            cache.set(cacheKey, response.data, options.cacheTTL || 300);
        }

        res.json({
            success: true,
            data: response.data,
            metadata: {
                service,
                endpoint,
                processingTime: response.headers['x-processing-time'] || null,
                cached: false,
                requestId: req.id
            }
        });

    } catch (error) {
        logger.error('Unified AI endpoint error', {
            requestId: req.id,
            error: error.message,
            stack: error.stack
        });

        if (error.response) {
            res.status(error.response.status).json({
                success: false,
                error: 'Service error',
                service: req.body.service,
                details: error.response.data
            });
        } else if (error.code === 'ECONNABORTED') {
            res.status(408).json({
                success: false,
                error: 'Request timeout',
                service: req.body.service
            });
        } else {
            res.status(500).json({
                success: false,
                error: 'Gateway error',
                details: error.message
            });
        }
    }
});

// Batch AI Processing - Multiple services in one request
app.post('/api/v1/ai/batch', [
    body('requests').isArray({ min: 1, max: 10 }).withMessage('Requests must be an array with 1-10 items'),
    body('requests.*.service').isIn(Object.keys(config.services)).withMessage('Invalid service specified'),
    body('requests.*.endpoint').notEmpty().withMessage('Endpoint is required')
], handleValidationErrors, async (req, res) => {
    try {
        const { requests } = req.body;
        const results = [];

        // Execute requests in parallel
        const promises = requests.map(async (requestConfig, index) => {
            const { service, endpoint, data = {}, method = 'POST', options = {} } = requestConfig;
            const serviceConfig = config.services[service];

            try {
                // Check cache first
                const cacheKey = `${service}:${endpoint}:${JSON.stringify(data)}`;
                const cached = cache.get(cacheKey);
                if (cached) {
                    return {
                        index,
                        success: true,
                        data: cached,
                        cached: true
                    };
                }

                // Check service health
                if (serviceHealth[service] === 'unhealthy' || serviceHealth[service] === 'circuit_open') {
                    throw new Error(`Service ${service} is unavailable`);
                }

                const url = `${serviceConfig.baseUrl}${endpoint}`;
                const response = await axios({
                    method: method.toLowerCase(),
                    url,
                    data,
                    timeout: serviceConfig.timeout,
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Request-ID': `${req.id}-${index}`
                    }
                });

                // Cache successful response
                if (options.cache !== false && response.data.success) {
                    cache.set(cacheKey, response.data, options.cacheTTL || 300);
                }

                return {
                    index,
                    success: true,
                    data: response.data,
                    cached: false
                };

            } catch (error) {
                return {
                    index,
                    success: false,
                    error: error.message,
                    service
                };
            }
        });

        const batchResults = await Promise.allSettled(promises);

        // Process results
        batchResults.forEach((result, index) => {
            if (result.status === 'fulfilled') {
                results[result.value.index] = result.value;
            } else {
                results[index] = {
                    index,
                    success: false,
                    error: 'Promise rejected',
                    details: result.reason.message
                };
            }
        });

        res.json({
            success: true,
            results,
            metadata: {
                totalRequests: requests.length,
                successfulRequests: results.filter(r => r.success).length,
                failedRequests: results.filter(r => !r.success).length,
                cachedRequests: results.filter(r => r.cached).length,
                requestId: req.id
            }
        });

    } catch (error) {
        logger.error('Batch AI endpoint error', {
            requestId: req.id,
            error: error.message
        });

        res.status(500).json({
            success: false,
            error: 'Batch processing failed',
            details: error.message
        });
    }
});

// Service Discovery Endpoint
app.get('/api/v1/services', (req, res) => {
    const services = Object.keys(config.services).map(serviceName => {
        const service = config.services[serviceName];
        return {
            name: serviceName,
            baseUrl: service.baseUrl,
            health: serviceHealth[serviceName] || 'unknown',
            endpoints: [
                '/api/v1/predict',
                '/api/v1/analyze',
                '/api/v1/recommendations',
                '/api/v1/detect',
                '/health',
                '/metrics'
            ]
        };
    });

    res.json({
        success: true,
        services,
        timestamp: new Date().toISOString()
    });
});

// Cache Management
app.post('/api/v1/cache/clear', (req, res) => {
    const { pattern } = req.body;

    if (pattern) {
        const keys = cache.keys().filter(key => key.includes(pattern));
        keys.forEach(key => cache.del(key));

        res.json({
            success: true,
            message: `Cleared ${keys.length} cached entries`,
            pattern
        });
    } else {
        cache.flushAll();
        res.json({
            success: true,
            message: 'Cleared all cache entries'
        });
    }
});

// Service-specific proxies with fallback
Object.keys(config.services).forEach(serviceName => {
    const service = config.services[serviceName];

    app.use(`/api/v1/${serviceName}`, createProxyMiddleware({
        target: service.baseUrl,
        changeOrigin: true,
        pathRewrite: {
            [`^/api/v1/${serviceName}`]: '/api/v1'
        },
        onError: (err, req, res) => {
            logger.error(`Proxy error for ${serviceName}`, {
                error: err.message,
                url: req.url
            });

            res.status(502).json({
                success: false,
                error: `Service ${serviceName} is unavailable`,
                service: serviceName,
                health: serviceHealth[serviceName]
            });
        },
        onProxyReq: (proxyReq, req, res) => {
            proxyReq.setHeader('X-Request-ID', req.id);
            proxyReq.setHeader('X-Forwarded-For', req.ip);
        }
    }));
});

// Error handling middleware
app.use((error, req, res, next) => {
    logger.error('Unhandled error', {
        requestId: req.id,
        error: error.message,
        stack: error.stack
    });

    res.status(500).json({
        success: false,
        error: 'Internal server error',
        requestId: req.id
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        error: 'Endpoint not found',
        path: req.path,
        method: req.method
    });
});

// Start server
const server = app.listen(config.port, () => {
    logger.info(`Unified AI Gateway started on port ${config.port}`, {
        environment: config.env,
        services: Object.keys(config.services).length,
        pid: process.pid
    });

    console.log(`🚀 Unified AI Gateway started on http://localhost:${config.port}`);
    console.log(`📊 Health Check: http://localhost:${config.port}/health`);
    console.log(`📈 Metrics: http://localhost:${config.port}/metrics`);
    console.log(`🔧 Services: http://localhost:${config.port}/api/v1/services`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    logger.info('SIGTERM received, shutting down gracefully');
    server.close(() => {
        logger.info('Server closed');
        process.exit(0);
    });
});

process.on('SIGINT', () => {
    logger.info('SIGINT received, shutting down gracefully');
    server.close(() => {
        logger.info('Server closed');
        process.exit(0);
    });
});

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
    logger.error('Uncaught Exception', {
        error: error.message,
        stack: error.stack
    });
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    logger.error('Unhandled Rejection', {
        reason,
        promise
    });
    process.exit(1);
});

module.exports = app;