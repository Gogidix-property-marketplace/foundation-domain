const express = require('express');
const router = express.Router();
const reportsController = require('../controllers/reportsController');
const { asyncHandler } = require('../middleware/errorHandler');
const auth = require('../middleware/auth');

// Apply authentication to all reports routes
router.use(auth.authenticate);

/**
 * @swagger
 * /api/v1/reports/generate:
 *   post:
 *     summary: Generate a new report
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - reportType
 *               - timeRange
 *             properties:
 *               reportType:
 *                 type: string
 *                 enum: [performance, usage, errors, model_comparison, service_health]
 *                 description: Type of report to generate
 *               timeRange:
 *                 type: object
 *                 properties:
 *                   start:
 *                     type: string
 *                     format: date-time
 *                   end:
 *                     type: string
 *                     format: date-time
 *               filters:
 *                 type: object
 *                 properties:
 *                   services:
 *                     type: array
 *                     items:
 *                       type: string
 *                   models:
 *                     type: array
 *                     items:
 *                       type: string
 *                   users:
 *                     type: array
 *                     items:
 *                       type: string
 *               format:
 *                 type: string
 *                 enum: [json, csv, pdf]
 *                 default: json
 *     responses:
 *       201:
 *         description: Report generated successfully
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
 *                     reportId:
 *                       type: string
 *                     status:
 *                       type: string
 *                       enum: [generating, completed, failed]
 *                     downloadUrl:
 *                       type: string
 */
router.post('/generate', asyncHandler(reportsController.generateReport));

/**
 * @swagger
 * /api/v1/reports:
 *   get:
 *     summary: Get list of available reports
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [generating, completed, failed]
 *         description: Filter by report status
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *         description: Filter by report type
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           description: Number of reports to return
 *         example: 20
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           description: Number of reports to skip
 *         example: 0
 *     responses:
 *       200:
 *         description: Reports list retrieved successfully
 */
router.get('/', asyncHandler(reportsController.getReports));

/**
 * @swagger
 * /api/v1/reports/{id}:
 *   get:
 *     summary: Get specific report details
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Report ID
 *     responses:
 *       200:
 *         description: Report details retrieved successfully
 *       404:
 *         description: Report not found
 */
router.get('/:id', asyncHandler(reportsController.getReportById));

/**
 * @swagger
 * /api/v1/reports/{id}/download:
 *   get:
 *     summary: Download report file
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Report ID
 *       - in: query
 *         name: format
 *         schema:
 *           type: string
 *           enum: [json, csv, pdf]
 *         description: Download format
 *     responses:
 *       200:
 *         description: Report file downloaded successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: string
 *           text/csv:
 *             schema:
 *               type: string
 *           application/pdf:
 *             schema:
 *               type: string
 */
router.get('/:id/download', asyncHandler(reportsController.downloadReport));

/**
 * @swagger
 * /api/v1/reports/scheduled:
 *   get:
 *     summary: Get scheduled reports
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Scheduled reports retrieved successfully
 */
router.get('/scheduled', asyncHandler(reportsController.getScheduledReports));

/**
 * @swagger
 * /api/v1/reports/scheduled:
 *   post:
 *     summary: Create a scheduled report
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - name
 *               - reportType
 *               - schedule
 *             properties:
 *               name:
 *                 type: string
 *                 description: Report name
 *               reportType:
 *                 type: string
 *                 enum: [performance, usage, errors, model_comparison, service_health]
 *               schedule:
 *                 type: object
 *                 properties:
 *                   frequency:
 *                     type: string
 *                     enum: [daily, weekly, monthly]
 *                   time:
 *                     type: string
 *                     pattern: ^([01]?[0-9]|2[0-3]):[0-5][0-9]$
 *                   dayOfWeek:
 *                     type: integer
 *                     minimum: 0
 *                     maximum: 6
 *                   dayOfMonth:
 *                     type: integer
 *                     minimum: 1
 *                     maximum: 31
 *               recipients:
 *                 type: array
 *                 items:
 *                   type: string
 *                   format: email
 *               format:
 *                 type: string
 *                 enum: [json, csv, pdf]
 *                 default: json
 *     responses:
 *       201:
 *         description: Scheduled report created successfully
 */
router.post('/scheduled', asyncHandler(reportsController.createScheduledReport));

/**
 * @swagger
 * /api/v1/reports/scheduled/{id}:
 *   put:
 *     summary: Update scheduled report
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Scheduled report ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *               schedule:
 *                 type: object
 *               recipients:
 *                 type: array
 *                 items:
 *                   type: string
 *     responses:
 *       200:
 *         description: Scheduled report updated successfully
 */
router.put('/scheduled/:id', asyncHandler(reportsController.updateScheduledReport));

/**
 * @swagger
 * /api/v1/reports/scheduled/{id}:
 *   delete:
 *     summary: Delete scheduled report
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Scheduled report ID
 *     responses:
 *       200:
 *         description: Scheduled report deleted successfully
 */
router.delete('/scheduled/:id', asyncHandler(reportsController.deleteScheduledReport));

/**
 * @swagger
 * /api/v1/reports/templates:
 *   get:
 *     summary: Get report templates
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Report templates retrieved successfully
 */
router.get('/templates', asyncHandler(reportsController.getReportTemplates));

/**
 * @swagger
 * /api/v1/reports/{id}/share:
 *   post:
 *     summary: Share report with users
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Report ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - emails
 *             properties:
 *               emails:
 *                 type: array
 *                 items:
 *                   type: string
 *                   format: email
 *               message:
 *                 type: string
 *               permissions:
 *                 type: string
 *                 enum: [view, download]
 *                 default: view
 *     responses:
 *       200:
 *         description: Report shared successfully
 */
router.post('/:id/share', asyncHandler(reportsController.shareReport));

module.exports = router;