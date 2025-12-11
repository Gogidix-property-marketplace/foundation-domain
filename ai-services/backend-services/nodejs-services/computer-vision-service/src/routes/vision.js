const express = require('express');
const router = express.Router();
const visionController = require('../controllers/visionController');
const { asyncHandler } = require('../middleware/errorHandler');
const auth = require('../middleware/auth');
const multer = require('multer');

// Configure multer for file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 50 * 1024 * 1024 // 50MB limit
  },
  fileFilter: (req, file, cb) => {
    if (file.mimetype.startsWith('image/') || file.mimetype.startsWith('video/')) {
      cb(null, true);
    } else {
      cb(new Error('Only image and video files are allowed'), false);
    }
  }
});

// Apply authentication to all routes
router.use(auth.authenticate);

/**
 * @swagger
 * /api/v1/vision/analyze:
 *   post:
 *     summary: Analyze an image using computer vision
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             required:
 *               - image
 *             properties:
 *               image:
 *                 type: string
 *                 format: binary
 *               imageUrl:
 *                 type: string
 *               analysisTypes:
 *                 type: array
 *                 items:
 *                   type: string
 *                   enum: [classification, object_detection, face_recognition, text_extraction, quality_analysis]
 *               modelId:
 *                 type: string
 *               propertyId:
 *                 type: string
 *     responses:
 *       202:
 *         description: Image analysis started
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
 *                     analysisId:
 *                       type: string
 *                     imageId:
 *                       type: string
 *                     status:
 *                       type: string
 *                     message:
 *                       type: string
 */
router.post('/analyze', upload.single('image'), asyncHandler(visionController.analyzeImage));

/**
 * @swagger
 * /api/v1/vision/analyze/{id}:
 *   get:
 *     summary: Get image analysis results
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Analysis ID
 *     responses:
 *       200:
 *         description: Analysis results retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   $ref: '#/components/schemas/ImageAnalysis'
 *       404:
 *         description: Analysis not found
 */
router.get('/analyze/:id', asyncHandler(visionController.getImageAnalysis));

/**
 * @swagger
 * /api/v1/vision/video/process:
 *   post:
 *     summary: Process a video for analysis
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             required:
 *               - video
 *             properties:
 *               video:
 *                 type: string
 *                 format: binary
 *               videoUrl:
 *                 type: string
 *               config:
 *                 type: object
 *                 properties:
 *                   analysisTypes:
 *                     type: array
 *                     items:
 *                       type: string
 *                   frameInterval:
 *                     type: integer
 *                     minimum: 1
 *                   confidenceThreshold:
 *                     type: number
 *                     minimum: 0
 *                     maximum: 1
 *               propertyId:
 *                 type: string
 *     responses:
 *       202:
 *         description: Video processing started
 */
router.post('/video/process', upload.single('video'), asyncHandler(visionController.processVideo));

/**
 * @swagger
 * /api/v1/vision/video/process/{id}:
 *   get:
 *     summary: Get video processing status
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: Processing ID
 *     responses:
 *       200:
 *         description: Processing status retrieved
 */
router.get('/video/process/:id', asyncHandler(visionController.getVideoProcessingStatus));

/**
 * @swagger
 * /api/v1/vision/detect/objects:
 *   post:
 *     summary: Detect objects in an image
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - imageUrl
 *             properties:
 *               imageUrl:
 *                 type: string
 *               modelId:
 *                 type: string
 *               confidenceThreshold:
 *                 type: number
 *                 minimum: 0
 *                 maximum: 1
 *                 default: 0.5
 *     responses:
 *       200:
 *         description: Objects detected successfully
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
 *                     modelId:
 *                       type: string
 *                     modelName:
 *                       type: string
 *                     detections:
 *                       type: array
 *                       items:
 *                         type: object
 *                         properties:
 *                           class:
 *                             type: string
 *                           confidence:
 *                             type: number
 *                           bbox:
 *                             type: array
 *                               items:
 *                                 type: number
 *                     count:
 *                       type: integer
 */
router.post('/detect/objects', asyncHandler(visionController.detectObjects));

/**
 * @swagger
 * /api/v1/vision/recognize/faces:
 *   post:
 *     summary: Recognize faces in an image
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - imageUrl
 *             properties:
 *               imageUrl:
 *                 type: string
 *               modelId:
 *                 type: string
 *               detectAttributes:
 *                 type: boolean
 *                 default: true
 *     responses:
 *       200:
 *         description: Faces recognized successfully
 */
router.post('/recognize/faces', asyncHandler(visionController.recognizeFaces));

/**
 * @swagger
 * /api/v1/vision/extract/text:
 *   post:
 *     summary: Extract text from image (OCR)
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - imageUrl
 *             properties:
 *               imageUrl:
 *                 type: string
 *               language:
 *                 type: string
 *                 default: eng
 *                 description: ISO 639-2 language code
 *     responses:
 *       200:
 *         description: Text extracted successfully
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
 *                     text:
 *                       type: string
 *                     words:
 *                       type: array
 *                       items:
 *                         type: object
 *                         properties:
 *                           text:
 *                             type: string
 *                           bbox:
 *                             type: array
 *                           confidence:
 *                             type: number
 *                     confidence:
 *                       type: number
 */
router.post('/extract/text', asyncHandler(visionController.extractText));

/**
 * @swagger
 * /api/v1/vision/models:
 *   get:
 *     summary: Get available computer vision models
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [classification, object_detection, face_recognition, ocr, scene_analysis, custom]
 *         description: Filter by model type
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [active, inactive, training, deprecated]
 *           default: active
 *         description: Filter by deployment status
 *     responses:
 *       200:
 *         description: Models retrieved successfully
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
 *                     models:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/VisionModel'
 *                     count:
 *                       type: integer
 */
router.get('/models', asyncHandler(visionController.getAvailableModels));

/**
 * @swagger
 * /api/v1/vision/analytics:
 *   get:
 *     summary: Get vision service analytics
 *     tags: [Vision]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: timeRange
 *         schema:
 *           type: integer
 *           default: 24
 *         description: Time range in hours
 *     responses:
 *       200:
 *         description: Analytics retrieved successfully
 */
router.get('/analytics', asyncHandler(visionController.getAnalytics));

/**
 * @swagger
 * /api/v1/vision/health:
 *   get:
 *     summary: Health check endpoint
 *     tags: [Vision]
 *     responses:
 *       200:
 *         description: Service is healthy
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 status:
 *                   type: string
 *                 timestamp:
 *                   type: string
 *                 uptime:
 *                   type: number
 *                 version:
 *                   type: string
 */
router.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    version: '1.0.0',
    service: 'computer-vision-service'
  });
});

module.exports = router;