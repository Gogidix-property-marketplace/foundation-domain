const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const { createProxyMiddleware } = require('http-proxy-middleware');
require('express-async-errors');
require('dotenv').config();

const logger = require('./utils/logger');
const errorHandler = require('./middleware/errorHandler');
const { connectDatabase } = require('./config/database');
const { setupSwagger } = require('./config/swagger');
const healthRoutes = require('./routes/health');
const metricsRoutes = require('./routes/metrics');

const app = express();

// Trust proxy for rate limiting
app.set('trust proxy', 1);

// Security middleware
app.use(helmet({
  crossOriginEmbedderPolicy: false,
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      scriptSrc: ["'self'"],
      imgSrc: ["'self'", "data:", "https:"],
    },
  },
}));

// CORS configuration
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With']
}));

// Compression middleware
app.use(compression());

// Request logging
app.use(morgan('combined', {
  stream: { write: message => logger.info(message.trim()) }
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 1000,
  message: {
    error: 'Too Many Requests',
    message: 'Rate limit exceeded. Please try again later.',
    retryAfter: Math.ceil((parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000) / 1000)
  },
  standardHeaders: true,
  legacyHeaders: false,
});

app.use(limiter);

// Body parsing middleware
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// API Documentation
setupSwagger(app);

// Routes
app.use('/health', healthRoutes);
app.use('/metrics', metricsRoutes);

// Proxy middleware to route to services
const services = [
  {
    path: '/api/v1/dashboard',
    target: 'http://localhost:3000',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/dashboard': '/api/v1'
    }
  },
  {
    path: '/api/v1/training',
    target: 'http://localhost:3001',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/training': '/api/v1'
    }
  },
  {
    path: '/api/v1/vision',
    target: 'http://localhost:3002',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/vision': '/api/v1'
    }
  },
  {
    path: '/api/v1/data-quality',
    target: 'http://localhost:3003',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/data-quality': '/api/v1'
    }
  },
  {
    path: '/api/v1/documents',
    target: 'http://localhost:3004',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/documents': '/api/v1'
    }
  },
  {
    path: '/api/v1/models',
    target: 'http://localhost:3005',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/models': '/api/v1'
    }
  },
  {
    path: '/api/v1/nlp',
    target: 'http://localhost:3006',
    changeOrigin: true,
    pathRewrite: {
      '^/api/v1/nlp': '/api/v1'
    }
  }
];

// Create proxy middleware for each service
services.forEach(service => {
  app.use(service.path, createProxyMiddleware({
    target: service.target,
    changeOrigin: service.changeOrigin,
    pathRewrite: service.pathRewrite,
    onError: (err, req, res) => {
      logger.error(`Proxy error for ${service.path}:`, err);
      res.status(503).json({
        error: 'Service Unavailable',
        message: `The ${service.path} service is currently unavailable`,
        timestamp: new Date().toISOString()
      });
    },
    onProxyReq: (proxyReq, req, res) => {
      logger.info(`Proxying ${req.method} ${req.originalUrl} to ${service.target}`);
    }
  }));
});

// Root endpoint
app.get('/', (req, res) => {
  res.json({
    message: '🚀 Gogidix AI Services Gateway',
    version: '1.0.0',
    services: {
      dashboard: 'http://localhost:3000',
      training: 'http://localhost:3001',
      vision: 'http://localhost:3002',
      'data-quality': 'http://localhost:3003',
      documents: 'http://localhost:3004',
      models: 'http://localhost:3005',
      nlp: 'http://localhost:3006'
    },
    documentation: '/api-docs',
    timestamp: new Date().toISOString()
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: `Route ${req.originalUrl} not found`,
    timestamp: new Date().toISOString(),
    path: req.originalUrl
  });
});

// Error handling middleware (must be last)
app.use(errorHandler);

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM received, shutting down gracefully');
  process.exit(0);
});

process.on('SIGINT', () => {
  logger.info('SIGINT received, shutting down gracefully');
  process.exit(0);
});

// Initialize database connection and start server
const PORT = process.env.PORT || 8000;

async function startServer() {
  try {
    // Connect to database
    await connectDatabase();
    logger.info('Database connected successfully');

    // Start HTTP server
    const server = app.listen(PORT, () => {
      logger.info(`🚀 AI Services Gateway is running on port ${PORT}`);
      logger.info(`📚 API Documentation: http://localhost:${PORT}/api-docs`);
      logger.info(`💚 Health Check: http://localhost:${PORT}/health`);
    });

    // Handle server errors
    server.on('error', (error) => {
      if (error.code === 'EADDRINUSE') {
        logger.error(`Port ${PORT} is already in use`);
      } else {
        logger.error('Server error:', error);
      }
      process.exit(1);
    });

    return server;
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Start server only if this file is run directly
if (require.main === module) {
  startServer();
}

module.exports = app;