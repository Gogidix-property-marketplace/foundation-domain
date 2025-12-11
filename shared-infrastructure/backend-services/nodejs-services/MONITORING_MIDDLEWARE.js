/**
 * Production Monitoring & Observability Middleware
 * Comprehensive monitoring implementation for Node.js services
 */

const prometheus = require('prom-client');
const { createLogger, transports, format } = require('winston');
const { ElasticsearchTransport } = require('winston-elasticsearch');
const { JaegerExporter } = require('@opentelemetry/exporter-jaeger');
const { NodeSDK } = require('@opentelemetry/sdk-node');
const { Resource } = require('@opentelemetry/resources');
const { SemanticResourceAttributes } = require('@opentelemetry/semantic-conventions');
const { ExpressInstrumentation } = require('@opentelemetry/instrumentation-express');
const { HttpInstrumentation } = require('@opentelemetry/instrumentation-http');

// Prometheus metrics setup
const createMetrics = (serviceName) => {
  // Create a Registry
  const register = new prometheus.Registry();

  // Add a default label
  register.setDefaultLabels({
    app: serviceName,
    env: process.env.NODE_ENV || 'development'
  });

  // Enable the collection of default metrics
  prometheus.collectDefaultMetrics({ register });

  // Custom metrics
  const httpRequestDuration = new prometheus.Histogram({
    name: 'http_request_duration_seconds',
    help: 'Duration of HTTP requests in seconds',
    labelNames: ['method', 'route', 'status_code'],
    buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10]
  });

  const httpRequestTotal = new prometheus.Counter({
    name: 'http_requests_total',
    help: 'Total number of HTTP requests',
    labelNames: ['method', 'route', 'status_code']
  });

  const httpRequestActive = new prometheus.Gauge({
    name: 'http_requests_active',
    help: 'Number of active HTTP requests'
  });

  const businessOperationsTotal = new prometheus.Counter({
    name: 'business_operations_total',
    help: 'Total number of business operations',
    labelNames: ['operation', 'status']
  });

  const activeUsers = new prometheus.Gauge({
    name: 'active_users',
    help: 'Number of active users'
  });

  const errorRate = new prometheus.Gauge({
    name: 'error_rate',
    help: 'Current error rate',
    labelNames: ['type']
  });

  register.registerMetric(httpRequestDuration);
  register.registerMetric(httpRequestTotal);
  register.registerMetric(httpRequestActive);
  register.registerMetric(businessOperationsTotal);
  register.registerMetric(activeUsers);
  register.registerMetric(errorRate);

  return {
    register,
    metrics: {
      httpRequestDuration,
      httpRequestTotal,
      httpRequestActive,
      businessOperationsTotal,
      activeUsers,
      errorRate
    }
  };
};

// Winston logger setup
const createLogger = (serviceName) => {
  const logFormat = format.combine(
    format.timestamp(),
    format.errors({ stack: true }),
    format.json(),
    format.prettyPrint()
  );

  const transports = [
    // Console transport for development
    new transports.Console({
      format: format.combine(
        format.colorize(),
        format.simple()
      )
    }),

    // File transport for errors
    new transports.File({
      filename: 'logs/error.log',
      level: 'error',
      format: logFormat,
      maxsize: 5242880, // 5MB
      maxFiles: 5
    }),

    // File transport for all logs
    new transports.File({
      filename: 'logs/combined.log',
      format: logFormat,
      maxsize: 5242880, // 5MB
      maxFiles: 5
    })
  ];

  // Add Elasticsearch transport in production
  if (process.env.NODE_ENV === 'production' && process.env.ELASTICSEARCH_URL) {
    transports.push(
      new ElasticsearchTransport({
        level: 'info',
        clientOpts: {
          node: process.env.ELASTICSEARCH_URL
        },
        index: `logs-${serviceName}-${new Date().toISOString().split('T')[0]}`
      })
    );
  }

  return createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: logFormat,
    defaultMeta: {
      service: serviceName,
      version: process.env.APP_VERSION || '1.0.0',
      environment: process.env.NODE_ENV || 'development'
    },
    transports
  });
};

// OpenTelemetry setup for distributed tracing
const initializeTracing = (serviceName) => {
  if (!process.env.TRACING_ENABLED || process.env.TRACING_ENABLED === 'false') {
    return null;
  }

  const sdk = new NodeSDK({
    resource: new Resource({
      [SemanticResourceAttributes.SERVICE_NAME]: serviceName,
      [SemanticResourceAttributes.SERVICE_VERSION]: process.env.APP_VERSION || '1.0.0',
      [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: process.env.NODE_ENV || 'development'
    }),
    traceExporter: new JaegerExporter({
      endpoint: process.env.JAEGER_ENDPOINT || 'http://localhost:14268/api/traces'
    }),
    instrumentations: [
      new ExpressInstrumentation(),
      new HttpInstrumentation()
    ]
  });

  sdk.start();

  return sdk;
};

// Create monitoring middleware
const createMonitoringMiddleware = (serviceName) => {
  const metrics = createMetrics(serviceName);
  const logger = createLogger(serviceName);
  const tracing = initializeTracing(serviceName);

  return {
    // Request metrics middleware
    requestMetrics: (req, res, next) => {
      const start = Date.now();

      // Increment active requests
      metrics.metrics.httpRequestActive.inc();

      res.on('finish', () => {
        const duration = (Date.now() - start) / 1000;
        const route = req.route ? req.route.path : req.path;

        // Record metrics
        metrics.metrics.httpRequestDuration
          .labels(req.method, route, res.statusCode.toString())
          .observe(duration);

        metrics.metrics.httpRequestTotal
          .labels(req.method, route, res.statusCode.toString())
          .inc();

        // Decrement active requests
        metrics.metrics.httpRequestActive.dec();

        // Log request
        logger.info('HTTP Request', {
          method: req.method,
          url: req.originalUrl,
          statusCode: res.statusCode,
          duration,
          userAgent: req.get('User-Agent'),
          ip: req.ip,
          requestId: req.id
        });

        // Update error rate
        if (res.statusCode >= 400) {
          const errorType = res.statusCode >= 500 ? 'server' : 'client';
          metrics.metrics.errorRate.labels(errorType).inc();
        }
      });

      next();
    },

    // Error tracking middleware
    errorTracking: (err, req, res, next) => {
      logger.error('Application Error', {
        error: err.message,
        stack: err.stack,
        url: req.originalUrl,
        method: req.method,
        body: req.body,
        params: req.params,
        query: req.query,
        headers: req.headers,
        ip: req.ip,
        requestId: req.id
      });

      metrics.metrics.businessOperationsTotal
        .labels('error', 'failed')
        .inc();

      next(err);
    },

    // Performance monitoring
    performanceMonitor: () => {
      const used = process.memoryUsage();
      const cpuUsage = process.cpuUsage();

      logger.info('Performance Metrics', {
        memory: {
          rss: `${Math.round(used.rss / 1024 / 1024)} MB`,
          heapTotal: `${Math.round(used.heapTotal / 1024 / 1024)} MB`,
          heapUsed: `${Math.round(used.heapUsed / 1024 / 1024)} MB`,
          external: `${Math.round(used.external / 1024 / 1024)} MB`
        },
        cpu: {
          user: cpuUsage.user,
          system: cpuUsage.system
        },
        uptime: process.uptime()
      });
    },

    // Health check endpoint
    healthCheck: (checks = {}) => {
      return async (req, res) => {
        const health = {
          status: 'UP',
          timestamp: new Date().toISOString(),
          service: serviceName,
          version: process.env.APP_VERSION || '1.0.0',
          uptime: process.uptime(),
          checks: {}
        };

        let overallStatus = 'UP';

        // Run health checks
        for (const [name, check] of Object.entries(checks)) {
          try {
            const result = await check();
            health.checks[name] = {
              status: 'UP',
              ...result
            };
          } catch (error) {
            health.checks[name] = {
              status: 'DOWN',
              error: error.message
            };
            overallStatus = 'DOWN';
          }
        }

        health.status = overallStatus;
        res.status(overallStatus === 'UP' ? 200 : 503).json(health);
      };
    },

    // Metrics endpoint
    metricsEndpoint: (req, res) => {
      res.set('Content-Type', metrics.register.contentType);
      res.end(metrics.register.metrics());
    },

    // Custom business metric
    trackBusinessOperation: (operation, status, metadata = {}) => {
      metrics.metrics.businessOperationsTotal
        .labels(operation, status)
        .inc();

      logger.info('Business Operation', {
        operation,
        status,
        ...metadata
      });
    },

    // Update active users
    updateActiveUsers: (count) => {
      metrics.metrics.activeUsers.set(count);
    },

    // Logger instance
    logger,

    // Metrics instance
    metrics,

    // Cleanup
    cleanup: () => {
      if (tracing) {
        tracing.shutdown();
      }
    }
  };
};

// Health check helpers
const createDatabaseHealthCheck = (pool) => {
  return async () => {
    const result = await pool.query('SELECT 1');
    return {
      status: 'UP',
      database: result.rowCount > 0 ? 'connected' : 'disconnected'
    };
  };
};

const createRedisHealthCheck = (redisClient) => {
  return async () => {
    const pong = await redisClient.ping();
    return {
      status: pong === 'PONG' ? 'UP' : 'DOWN',
      response: pong
    };
  };
};

const createExternalServiceHealthCheck = (url, serviceName) => {
  return async () => {
    const response = await fetch(url, {
      method: 'GET',
      timeout: 5000
    });
    return {
      status: response.ok ? 'UP' : 'DOWN',
      statusCode: response.status,
      serviceName
    };
  };
};

module.exports = {
  createMonitoringMiddleware,
  createDatabaseHealthCheck,
  createRedisHealthCheck,
  createExternalServiceHealthCheck
};