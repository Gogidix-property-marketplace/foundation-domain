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
    message: 'Welcome to document-analysis-service API',
    version: '1.0.0',
    service: 'document-analysis-service',
    timestamp: new Date().toISOString()
  });
});

// Import and use specific routes
// const userRoutes = require('./users');
// const authRoutes = require('./auth');

const docRoutes = require('./document');
router.use('/document', docRoutes);('/users', userRoutes);
const docRoutes = require('./document');
router.use('/document', docRoutes);('/auth', authRoutes);

module.exports = router;
