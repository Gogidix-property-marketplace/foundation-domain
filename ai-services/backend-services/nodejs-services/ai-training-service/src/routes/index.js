const express = require('express');
const router = express.Router();

/**
 * @swagger
 * /api/v1:
 *   get:
 *     summary: Get API information
 *     tags: [API]
 *     responses:
 *       200:
 *         description: API information
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                 version:
 *                   type: string
 *                 service:
 *                   type: string
 */
router.get('/', (req, res) => {
  res.json({
    message: 'Welcome to ai-training-service API',
    version: '1.0.0',
    service: 'ai-training-service',
    timestamp: new Date().toISOString()
  });
});

// Import and use specific routes
const trainingRoutes = require('./training');

// Mount routes
router.use('/training', trainingRoutes);

module.exports = router;
