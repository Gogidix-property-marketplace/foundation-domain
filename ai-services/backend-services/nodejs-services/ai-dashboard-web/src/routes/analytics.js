const express = require('express');
const router = express.Router();
const analyticsController = require('../controllers/analyticsController');
const { asyncHandler } = require('../middleware/errorHandler');
const auth = require('../middleware/auth');

// Apply authentication to all analytics routes
router.use(auth.authenticate);

/**
 * @swagger
 * /api/v1/analytics/metrics:
 *   get:
 *     summary: Get real-time metrics for AI services
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Real-time metrics retrieved successfully
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
 *                     metrics:
 *                       type: object
 *                     timestamp:
 *                       type: string
 *                     timeRange:
 *                       type: number
 *       401:
 *         description: Unauthorized
 */
router.get('/metrics', asyncHandler(analyticsController.getRealTimeMetrics));

/**
 * @swagger
 * /api/v1/analytics/summary:
 *   get:
 *     summary: Get analytics summary
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *         description: Time range in minutes
 *         example: 1440
 *     responses:
 *       200:
 *         description: Analytics summary retrieved successfully
 */
router.get('/summary', asyncHandler(analyticsController.getAnalyticsSummary));

/**
 * @swagger
 * /api/v1/analytics/models:
 *   get:
 *     summary: Get model performance analytics
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: modelId
 *         schema:
 *           type: string
 *         description: Filter by model ID
 *       - in: query
 *         name: serviceName
 *         schema:
 *           type: string
 *         description: Filter by service name
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *         description: Time range in minutes
 *     responses:
 *       200:
 *         description: Model performance analytics
 */
router.get('/models', asyncHandler(analyticsController.getModelPerformance));

/**
 * @swagger
 * /api/v1/analytics/users:
 *   get:
 *     summary: Get user analytics
 *     tags: [     *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: userId
 *         schema:
 *           type: string
 *         description: Filter by user ID
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *         description: Time range in minutes
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           description: Number of records to return
 *     responses:
 *       200:
 *         description: User analytics data
 */
router.get('/users', asyncHandler(analyticsController.getUserAnalytics));

/**
 * @swagger
 * /api/v1/analytics/widgets:
 *   get:
 *     summary: Get dashboard widgets
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: isVisible
 *         schema:
 *           type: boolean
 *         description: Filter by visibility
 *     responses:
 *       200:
 *         description: Dashboard widgets
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
 *                     widgets:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/DashboardWidget'
 *                     count:
 *                       type: number
 */
 */
router.get('/widgets', asyncHandler(analyticsController.getDashboardWidgets));

/**
 * @swagger
 * /api/v1/analytics/widgets:
 *   post:
 *     summary: Create a new dashboard widget
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/DashboardWidgetInput'
 *     responses:
 *       201:
 *         description: Widget created successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/DashboardWidget'
 */
router.post('/widgets', asyncHandler(analyticsController.createDashboardWidget));

/**
 * @swagger
 * /api/v1/analytics/widgets/:id:
 *   put:
 *     summary: Update a dashboard widget
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Widget ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/DashboardWidgetUpdate'
 *     responses:
 *       200:
 *         description: Widget updated successfully
 *       404:
 *         description: Widget not found
 */
router.put('/widgets/:id', asyncHandler(analyticsController.updateDashboardWidget));

/**
 * @swagger
 * /api/v1/analytics/widgets/:id:
 *   delete:
 *     summary: Delete a dashboard widget
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Widget ID
 *     responses:
 *       200:
 *         description: Widget deleted successfully
 *       404:
 *         description: Widget not found
 */
router.delete('/widgets/:id', asyncHandler(analyticsController.deleteDashboardWidget));

/**
 * @swagger
 * /api/v1/analytics/export:
 *   get:
 *     summary: Export analytics data
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: format
 *         schema:
 *           type: string
 *           enum: [json, csv]
 *         description: Export format
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *         description: Time range in minutes
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [all, events, performance, realtime, users]
 *         description: Type of data to export
 *     responses:
 *       200:
 *         description: Analytics data exported
 *         content:
 *           application/json:
 *             type: object
 *           text/csv:
 *             type: string
 */
router.get('/export', asyncHandler(analyticsController.exportAnalytics));

/**
 * @swagger
 * /api/v1/analytics/events:
 *   post:
 *     summary: Create analytics event
 *     tags: [Analytics]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - eventType
 *               - serviceName
 *               - properties
 *             properties:
 *               eventType:
 *                 type: string
 *                 enum: [model_prediction, model_training, user_interaction, system_alert, performance_metric, error_event, data_processing]
 *               serviceName:
 *                 type: string
 *                 enum: [dashboard, training, vision, data-quality, documents, models, nlp]
 *               properties:
 *                 type: object
 *                 description: Event properties
 *               userId:
 *                 type: string
 *                 description: User ID
 *               sessionId:
 *                 type: string
 *                 description: Session ID
 *               metrics:
 *                 type: object
 *                 properties:
 *                   responseTime:
 *                     type: number
 *                   throughput:
 *                     type: number
 *                   accuracy:
 *                     type: number
 *                   errorRate:
 *                     type: number
 *                   resourceUsage:
 *                     type: object
 *                 status:
 *                   type: string
 *                   enum: [success, error, pending]
 *                 metadata:
 *                   type: object
 *             example:
 *               eventType: "model_prediction"
 *               serviceName: "models"
 *               properties:
 *                 modelId: "model_123"
 *                 prediction: 0.95
 *                 inputType: "image"
 *               metrics:
 *                 responseTime: 150
 *                 accuracy: 0.92
 *               status: "success"
 *     responses:
 *       201:
 *         description: Event created successfully
 *       400:
 *         description: Bad request
 */
router.post('/events', asyncHandler(async (req, res, next) => {
  try {
    const { AnalyticsEvent } = require('../models/Analytics');
    const eventData = {
      ...req.body,
      timestamp: new Date(),
      status: req.body.status || 'success'
    };

    const event = new AnalyticsEvent(eventData);
    await event.save();

    res.status(201).json({
      success: true,
      data: event
    });
  } catch (error) {
    next(error);
  }
}));

module.exports = router;