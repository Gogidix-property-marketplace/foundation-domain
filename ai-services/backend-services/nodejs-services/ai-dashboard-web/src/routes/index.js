const express = require('express');
const router = express.Router();

// Import specific routes
const analyticsRoutes = require('./analytics');
const dashboardRoutes = require('./dashboard');
const reportsRoutes = require('./reports');

/**
 * @swagger
 * /api/v1:
 *   get:
 *     summary: Get API information
 *     tags: [Dashboard]
 *     responses:
 *       200:
 *         description: API information
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type:       string
 *                 version:
 *                   type:       string
 *                 service:
 *                   type:       string
 *                 features:
 *                   type:       array
 *                   description: Array of available features
 *                   items:
 *                     type: string
 */
router.get('/', (req, res) => {
  res.json({
    message: 'Welcome to AI Dashboard Web API',
    version: '1.0.0',
    service: 'ai-dashboard-web',
    features: [
      'Real-time analytics dashboard',
      'Model performance monitoring',
      'User behavior tracking',
      'Customizable widgets',
      'Automated reports',
      'Data export capabilities'
    ],
    timestamp: new Date().toISOString()
  });
});

/**
 * @swagger
 * /api/v1/health:
 *   get:
 *     summary: Health check
 *     tags: [Dashboard]
 *     responses:
 *       200:
 *         description: Service is healthy
 */
router.get('/health', (req, res) => {
  res.status(200).json({
    status: 'OK',
    timestamp: new Date().toISOString(),
    service: 'ai-dashboard-web',
    version: '1.0.0',
    uptime: process.uptime(),
    environment: process.env.NODE_ENV || 'development'
  });
});

// Mount routes
router.use('/analytics', analyticsRoutes);
router.use('/dashboard', dashboardRoutes);
router.use('/reports', reportsRoutes);

module.exports = router;
