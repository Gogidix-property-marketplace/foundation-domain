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
    message: 'Welcome to natural-language-processing-service API',
    version: '1.0.0',
    service: 'natural-language-processing-service',
    timestamp: new Date().toISOString()
  });
});

// Import and use specific routes
// const userRoutes = require('./users');
// const authRoutes = require('./auth');

const nlpRoutes = require('./nlp');
router.use('/nlp', nlpRoutes);('/users', userRoutes);
const nlpRoutes = require('./nlp');
router.use('/nlp', nlpRoutes);('/auth', authRoutes);

module.exports = router;
