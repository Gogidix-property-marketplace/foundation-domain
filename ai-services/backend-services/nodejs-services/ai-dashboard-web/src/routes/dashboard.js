const express = require('express');
const router = express.Router();
const dashboardController = require('../controllers/dashboardController');
const { asyncHandler } = require('../middleware/errorHandler');
const auth = require('../middleware/auth');

// Apply authentication to all dashboard routes
router.use(auth.authenticate);

/**
 * @swagger
 * /api/v1/dashboard/overview:
 *   get:
 *     summary: Get dashboard overview with key metrics
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Dashboard overview data retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: object
 *                   properties:
 *                     totalModels:
 *                       type: number
 *                     activeTrainings:
 *                       type: number
 *                     predictionsToday:
 *                       type: number
 *                     systemHealth:
 *                       type: string
 *                     services:
 *                       type: array
 *                       items:
 *                         type: object
 *                         properties:
 *                           name:
 *                             type: string
 *                           status:
 *                             type: string
 *                           lastUpdated:
 *                             type: string
 */
router.get('/overview', asyncHandler(dashboardController.getDashboardOverview));

/**
 * @swagger
 * /api/v1/dashboard/services:
 *   get:
 *     summary: Get status of all AI services
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Services status retrieved successfully
 */
router.get('/services', asyncHandler(dashboardController.getServicesStatus));

/**
 * @swagger
 * /api/v1/dashboard/metrics/summary:
 *   get:
 *     summary: Get metrics summary for dashboard
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *         description: Time range in minutes
 *         example: 1440
 *       - in: query
 *         name: services
 *         schema:
 *           type: string
 *         description: Comma-separated list of services
 *     responses:
 *       200:
 *         description: Metrics summary retrieved successfully
 */
router.get('/metrics/summary', asyncHandler(dashboardController.getMetricsSummary));

/**
 * @swagger
 * /api/v1/dashboard/alerts:
 *   get:
 *     summary: Get system alerts and notifications
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: severity
 *         schema:
 *           type: string
 *           enum: [low, medium, high, critical]
 *         description: Filter by severity level
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           description: Number of alerts to return
 *         example: 50
 *     responses:
 *       200:
 *         description: Alerts retrieved successfully
 */
router.get('/alerts', asyncHandler(dashboardController.getAlerts));

/**
 * @swagger
 * /api/v1/dashboard/performance:
 *   get:
 *     summary: Get system performance metrics
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *           description: Time range in minutes
 *         example: 60
 *     responses:
 *       200:
 *         description: Performance metrics retrieved successfully
 */
router.get('/performance', asyncHandler(dashboardController.getPerformanceMetrics));

/**
 * @swagger
 * /api/v1/dashboard/user/activity:
 *   get:
 *     summary: Get user activity statistics
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *           description: Time range in minutes
 *         example: 1440
 *       - in: query
 *         name: userId
 *         schema:
 *           type: string
 *         description: Filter by specific user ID
 *     responses:
 *       200:
 *         description: User activity statistics retrieved successfully
 */
router.get('/user/activity', asyncHandler(dashboardController.getUserActivity));

/**
 * @swagger
 * /api/v1/dashboard/models/top:
 *   get:
 *     summary: Get top performing models
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: metric
 *         schema:
 *           type: string
 *           enum: [accuracy, performance, usage]
 *         description: Metric to rank models by
 *         example: accuracy
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           description: Number of models to return
 *         example: 10
 *     responses:
 *       200:
 *         description: Top performing models retrieved successfully
 */
router.get('/models/top', asyncHandler(dashboardController.getTopModels));

/**
 * @swagger
 * /api/v1/dashboard/trends:
 *   get:
 *     summary: Get usage and performance trends
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: metric
 *         schema:
 *           type: string
 *           enum: [predictions, accuracy, response_time, errors]
 *         description: Trend metric
 *         example: predictions
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *           description: Time range in minutes
 *         example: 10080
 *     responses:
 *       200:
 *         description: Trend data retrieved successfully
 */
router.get('/trends', asyncHandler(dashboardController.getTrends));

/**
 * @swagger
 * /api/v1/dashboard/health/detailed:
 *   get:
 *     summary: Get detailed system health information
 *     tags: [Dashboard]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Detailed system health information retrieved successfully
 */
router.get('/health/detailed', asyncHandler(dashboardController.getDetailedHealth));

module.exports = router;