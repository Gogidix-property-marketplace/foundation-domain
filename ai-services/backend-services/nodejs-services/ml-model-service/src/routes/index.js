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
    message: 'Welcome to ml-model-service API',
    version: '1.0.0',
    service: 'ml-model-service',
    timestamp: new Date().toISOString()
  });
});

// Import and use specific routes
// const userRoutes = require('./users');
// const authRoutes = require('./auth');

const modelRoutes = require('./model');
router.use('/model', modelRoutes);('/users', userRoutes);
const modelRoutes = require('./model');
router.use('/model', modelRoutes);('/auth', authRoutes);

module.exports = router;
