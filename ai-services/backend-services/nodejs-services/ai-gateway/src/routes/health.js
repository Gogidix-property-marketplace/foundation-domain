const express = require('express');
const router = express.Router();
const logger = require('../utils/logger');
const { getServiceHealth } = require('../utils/serviceRegistry');

/**
 * @swagger
 * /health:
 *   get:
 *     summary: Get health status of all AI services
 *     tags: [Health]
 *     responses:
 *       200:
 *         description: Health status of all services
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 status:
 *                   type: string
 *                   example: healthy
 *                 gateway:
 *                   type: object
 *                   properties:
 *                     status:
 *                       type: string
 *                     timestamp:
 *                       type: string
 *                     uptime:
 *                       type: number
 *                 services:
 *                   type: object
 *                   properties:
 *                     dashboard:
 *                       type: object
 *                       properties:
 *                         status:
 *                           type: string
 *                         responseTime:
 *                           type: number
 *                         lastCheck:
 *                           type: string
 *                     training:
 *                       type: object
 *                     vision:
 *                       type: object
 *                     data-quality:
 *                       type: object
 *                     documents:
 *                       type: object
 *                     models:
 *                       type: object
 *                     nlp:
 *                       type: object
 */
router.get('/', async (req, res) => {
  try {
    const timestamp = new Date().toISOString();

    // Get health status of all services
    const serviceHealths = await getServiceHealth();

    // Determine overall status
    const allHealthy = Object.values(serviceHealths).every(
      service => service.status === 'healthy'
    );

    const healthStatus = {
      status: allHealthy ? 'healthy' : 'degraded',
      timestamp,
      gateway: {
        status: 'healthy',
        uptime: process.uptime(),
        memory: process.memoryUsage(),
        cpu: process.cpuUsage()
      },
      services: serviceHealths
    };

    const statusCode = allHealthy ? 200 : 503;
    res.status(statusCode).json(healthStatus);

  } catch (error) {
    logger.error('Health check error:', error);
    res.status(500).json({
      status: 'unhealthy',
      timestamp: new Date().toISOString(),
      error: error.message
    });
  }
});

/**
 * @swagger
 * /health/ready:
 *   get:
 *     summary: Check if services are ready for traffic
 *     tags: [Health]
 *     responses:
 *       200:
 *         description: Services are ready
 *       503:
 *         description: Services are not ready
 */
router.get('/ready', async (req, res) => {
  try {
    const serviceHealths = await getServiceHealth();

    // Check if all critical services are ready
    const criticalServices = ['dashboard', 'training', 'models'];
    const allReady = criticalServices.every(
      service => serviceHealths[service] && serviceHealths[service].status === 'healthy'
    );

    if (allReady) {
      res.status(200).json({
        status: 'ready',
        timestamp: new Date().toISOString(),
        checks: {
          gateway: 'healthy',
          ...criticalServices.reduce((acc, service) => {
            acc[service] = serviceHealths[service].status;
            return acc;
          }, {})
        }
      });
    } else {
      res.status(503).json({
        status: 'not ready',
        timestamp: new Date().toISOString(),
        checks: criticalServices.reduce((acc, service) => {
          acc[service] = serviceHealths[service]?.status || 'unknown';
          return acc;
        }, {})
      });
    }
  } catch (error) {
    logger.error('Readiness check error:', error);
    res.status(503).json({
      status: 'not ready',
      timestamp: new Date().toISOString(),
      error: error.message
    });
  }
});

/**
 * @swagger
 * /health/live:
 *   get:
 *     summary: Check if the gateway process is alive
 *     tags: [Health]
 *     responses:
 *       200:
 *         description: Process is alive
 */
router.get('/live', (req, res) => {
  res.status(200).json({
    status: 'alive',
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});

/**
 * @swagger
 * /health/deep:
 *   get:
 *     summary: Deep health check with detailed diagnostics
 *     tags: [Health]
 *     responses:
 *       200:
 *         description: Detailed health information
 */
router.get('/deep', async (req, res) => {
  try {
    const serviceHealths = await getServiceHealth();

    const deepHealth = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      gateway: {
        status: 'healthy',
        uptime: process.uptime(),
        memory: process.memoryUsage(),
        cpu: process.cpuUsage(),
        nodeVersion: process.version,
        platform: process.platform,
        arch: process.arch,
        pid: process.pid
      },
      services: serviceHealths,
      environment: {
        NODE_ENV: process.env.NODE_ENV,
        PORT: process.env.PORT,
        LOG_LEVEL: process.env.LOG_LEVEL
      },
      diagnostics: {
        memoryUsage: process.memoryUsage(),
        heapUsed: process.memoryUsage().heapUsed / 1024 / 1024 + ' MB',
        heapTotal: process.memoryUsage().heapTotal / 1024 / 1024 + ' MB',
        external: process.memoryUsage().external / 1024 / 1024 + ' MB'
      }
    };

    res.status(200).json(deepHealth);

  } catch (error) {
    logger.error('Deep health check error:', error);
    res.status(500).json({
      status: 'unhealthy',
      timestamp: new Date().toISOString(),
      error: error.message
    });
  }
});

module.exports = router;