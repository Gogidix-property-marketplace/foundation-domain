const express = require('express');
const router = express.Router();
const client = require('prom-client');
const logger = require('../utils/logger');

// Create a Registry to register the metrics
const register = new client.Registry();

// Add a default label which is used by Prometheus
register.setDefaultLabels({
  app: 'ai-gateway'
});

// Enable the collection of default metrics
client.collectDefaultMetrics({ register });

// Create custom metrics
const httpRequestDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code', 'service'],
  buckets: [0.1, 0.5, 1, 1.5, 2, 5, 10]
});

const httpRequestCount = new client.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code', 'service']
});

const httpRequestErrors = new client.Counter({
  name: 'http_request_errors_total',
  help: 'Total number of HTTP request errors',
  labelNames: ['method', 'route', 'error_type', 'service']
});

const activeConnections = new client.Gauge({
  name: 'active_connections',
  help: 'Number of active connections'
});

const serviceHealthStatus = new client.Gauge({
  name: 'service_health_status',
  help: 'Health status of AI services (1 = healthy, 0 = unhealthy)',
  labelNames: ['service']
});

const apiResponseTime = new client.Histogram({
  name: 'api_response_time_seconds',
  help: 'Response time of downstream API calls',
  labelNames: ['service', 'endpoint'],
  buckets: [0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10]
});

// Register the metrics
register.registerMetric(httpRequestDuration);
register.registerMetric(httpRequestCount);
register.registerMetric(httpRequestErrors);
register.registerMetric(activeConnections);
register.registerMetric(serviceHealthStatus);
register.registerMetric(apiResponseTime);

// Middleware to track request metrics
const requestMetrics = (req, res, next) => {
  const start = Date.now();

  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    const route = req.route ? req.route.path : req.path;

    httpRequestDuration
      .withLabels({
        method: req.method,
        route,
        status_code: res.statusCode,
        service: req.service || 'gateway'
      })
      .observe(duration);

    httpRequestCount
      .withLabels({
        method: req.method,
        route,
        status_code: res.statusCode,
        service: req.service || 'gateway'
      })
      .inc();

    if (res.statusCode >= 400) {
      httpRequestErrors
        .withLabels({
          method: req.method,
          route,
          error_type: res.statusCode >= 500 ? 'server_error' : 'client_error',
          service: req.service || 'gateway'
        })
        .inc();
    }
  });

  next();
};

// Function to update service health status
const updateServiceHealth = (serviceName, isHealthy) => {
  serviceHealthStatus
    .withLabels({ service: serviceName })
    .set(isHealthy ? 1 : 0);
};

// Function to record API response time
const recordApiResponseTime = (serviceName, endpoint, duration) => {
  apiResponseTime
    .withLabels({ service: serviceName, endpoint })
    .observe(duration);
};

/**
 * @swagger
 * /metrics:
 *   get:
 *     summary: Get Prometheus metrics
 *     tags: [Metrics]
 *     produces:
 *       - text/plain
 *     responses:
 *       200:
 *         description: Prometheus metrics
 *         content:
 *           text/plain:
 *             schema:
 *               type: string
 */
router.get('/', (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(register.metrics());
});

/**
 * @swagger
 * /metrics/api-response-time:
 *   get:
 *     summary: Get API response time metrics
 *     tags: [Metrics]
 *     responses:
 *       200:
 *         description: Response time metrics
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 */
router.get('/api-response-time', async (req, res) => {
  try {
    const metrics = await apiResponseTime.get();
    res.json({
      name: metrics.name,
      help: metrics.help,
      values: Array.from(await metrics.get()).map(([key, value]) => ({
        metric: key,
        value: value
      }))
    });
  } catch (error) {
    logger.error('Error fetching API response time metrics:', error);
    res.status(500).json({ error: 'Failed to fetch metrics' });
  }
});

/**
 * @swagger
 * /metrics/health-status:
 *   get:
 *     summary: Get service health status metrics
 *     tags: [Metrics]
 *     responses:
 *       200:
 *         description: Health status metrics
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 */
router.get('/health-status', async (req, res) => {
  try {
    const metrics = await serviceHealthStatus.get();
    res.json({
      name: metrics.name,
      help: metrics.help,
      values: Array.from(await metrics.get()).map(([key, value]) => ({
        service: key.split('{')[0].trim(),
        healthy: value === 1
      }))
    });
  } catch (error) {
    logger.error('Error fetching health status metrics:', error);
    res.status(500).json({ error: 'Failed to fetch metrics' });
  }
});

/**
 * @swagger
 * /metrics/requests:
 *   get:
 *     summary: Get HTTP request metrics
 *     tags: [Metrics]
 *     responses:
 *       200:
 *         description: HTTP request metrics
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 */
router.get('/requests', async (req, res) => {
  try {
    const countMetrics = await httpRequestCount.get();
    const durationMetrics = await httpRequestDuration.get();

    res.json({
      total_requests: Array.from(await countMetrics.get()).reduce((sum, [, value]) => sum + value, 0),
      avg_response_time: calculateAverage(Array.from(await durationMetrics.get()).map(([, value]) => value)),
      requests_by_status: groupByStatus(Array.from(await countMetrics.get())),
      response_time_histogram: Array.from(await durationMetrics.get()).map(([key, value]) => ({
        bucket: key,
        count: value
      }))
    });
  } catch (error) {
    logger.error('Error fetching request metrics:', error);
    res.status(500).json({ error: 'Failed to fetch metrics' });
  }
});

/**
 * @swagger
 * /metrics/errors:
 *   get:
 *     summary: Get error metrics
 *     tags: [Metrics]
 *     responses:
 *       200:
 *         description: Error metrics
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 */
router.get('/errors', async (req, res) => {
  try {
    const errorMetrics = await httpRequestErrors.get();

    res.json({
      total_errors: Array.from(await errorMetrics.get()).reduce((sum, [, value]) => sum + value, 0),
      errors_by_type: groupByErrorType(Array.from(await errorMetrics.get())),
      errors_by_service: groupByService(Array.from(await errorMetrics.get()))
    });
  } catch (error) {
    logger.error('Error fetching error metrics:', error);
    res.status(500).json({ error: 'Failed to fetch metrics' });
  }
});

// Helper functions
function calculateAverage(values) {
  if (values.length === 0) return 0;
  const sum = values.reduce((acc, val) => acc + val, 0);
  return sum / values.length;
}

function groupByStatus(metrics) {
  return metrics.reduce((acc, [key, value]) => {
    const status = key.match(/status_code="([^"]+)"/)?.[1] || 'unknown';
    acc[status] = (acc[status] || 0) + value;
    return acc;
  }, {});
}

function groupByErrorType(metrics) {
  return metrics.reduce((acc, [key, value]) => {
    const errorType = key.match(/error_type="([^"]+)"/)?.[1] || 'unknown';
    acc[errorType] = (acc[errorType] || 0) + value;
    return acc;
  }, {});
}

function groupByService(metrics) {
  return metrics.reduce((acc, [key, value]) => {
    const service = key.match(/service="([^"]+)"/)?.[1] || 'unknown';
    acc[service] = (acc[service] || 0) + value;
    return acc;
  }, {});
}

module.exports = {
  router,
  register,
  requestMetrics,
  updateServiceHealth,
  recordApiResponseTime,
  activeConnections
};