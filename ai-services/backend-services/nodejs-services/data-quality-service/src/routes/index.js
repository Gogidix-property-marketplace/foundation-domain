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
    message: 'Welcome to data-quality-service API',
    version: '1.0.0',
    service: 'data-quality-service',
    timestamp: new Date().toISOString()
  });
});

// Import and use specific routes
// const userRoutes = require('./users');
// const authRoutes = require('./auth');

const qualityRoutes = require('./quality');
router.use('/quality', qualityRoutes);('/users', userRoutes);
const qualityRoutes = require('./quality');
router.use('/quality', qualityRoutes);('/auth', authRoutes);

module.exports = router;
