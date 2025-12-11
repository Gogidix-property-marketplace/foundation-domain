/**
 * Production Security Middleware Template
 * Comprehensive security implementation for Node.js services
 */

const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const slowDown = require('express-slow-down');
const xss = require('xss');
const mongoSanitize = require('express-mongo-sanitize');
const hpp = require('hpp');
const compression = require('compression');
const validator = require('validator');

// Custom security middleware factory
const createSecurityMiddleware = (options = {}) => {
  const {
    corsOrigins = process.env.CORS_ORIGINS?.split(',') || ['http://localhost:3000'],
    rateLimitWindowMs = 15 * 60 * 1000, // 15 minutes
    rateLimitMax = 100, // 100 requests per window
    trustProxy = process.env.TRUST_PROXY === 'true'
  } = options;

  const securityMiddleware = [];

  // 1. Helmet for security headers
  securityMiddleware.push(
    helmet({
      contentSecurityPolicy: {
        directives: {
          defaultSrc: ["'self'"],
          styleSrc: ["'self'", "'unsafe-inline'", 'https://fonts.googleapis.com'],
          fontSrc: ["'self'", 'https://fonts.gstatic.com'],
          imgSrc: ["'self'", 'data:', 'https:'],
          scriptSrc: ["'self'"],
          connectSrc: ["'self'"],
          frameSrc: ["'none'"],
          objectSrc: ["'none'"]
        }
      },
      hsts: {
        maxAge: 31536000,
        includeSubDomains: true,
        preload: true
      },
      noSniff: true,
      frameguard: { action: 'deny' },
      ieNoOpen: true,
      dnsPrefetchControl: false
    })
  );

  // 2. CORS configuration
  securityMiddleware.push(
    cors({
      origin: (origin, callback) => {
        // Allow requests with no origin (like mobile apps or curl requests)
        if (!origin) return callback(null, true);

        if (corsOrigins.includes(origin) || process.env.NODE_ENV !== 'production') {
          callback(null, true);
        } else {
          callback(new Error('Not allowed by CORS'));
        }
      },
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
      allowedHeaders: [
        'Origin',
        'X-Requested-With',
        'Content-Type',
        'Accept',
        'Authorization',
        'X-API-Key',
        'X-Request-ID'
      ],
      exposedHeaders: ['X-Total-Count', 'X-Page-Count'],
      maxAge: 86400 // 24 hours
    })
  );

  // 3. Rate limiting
  const limiter = rateLimit({
    windowMs: rateLimitWindowMs,
    max: rateLimitMax,
    message: {
      error: 'Too many requests from this IP, please try again later.',
      retryAfter: Math.ceil(rateLimitWindowMs / 1000)
    },
    standardHeaders: true,
    legacyHeaders: false,
    skip: (req) => {
      // Skip rate limiting for health checks
      return req.path === '/health' || req.path === '/ready';
    }
  });
  securityMiddleware.push(limiter);

  // 4. Slow down for repeated requests
  const speedLimiter = slowDown({
    windowMs: rateLimitWindowMs,
    delayAfter: 50, // Allow 50 requests per 15min at full speed
    delayMs: 500, // Add 500ms delay per request after delayAfter
    maxDelayMs: 5000, // Maximum delay of 5 seconds
    skip: (req) => {
      return req.path === '/health' || req.path === '/ready';
    }
  });
  securityMiddleware.push(speedLimiter);

  // 5. Request size limiting
  securityMiddleware.push((req, res, next) => {
    const contentLength = req.get('content-length');
    const maxSize = 10 * 1024 * 1024; // 10MB

    if (contentLength && parseInt(contentLength) > maxSize) {
      return res.status(413).json({
        error: 'Request entity too large',
        maxSize: `${maxSize / 1024 / 1024}MB`
      });
    }
    next();
  });

  // 6. Request sanitization
  securityMiddleware.push(mongoSanitize()); // Prevent NoSQL injection
  securityMiddleware.push(hpp()); // Prevent HTTP parameter pollution

  // 7. Custom XSS protection
  securityMiddleware.push((req, res, next) => {
    if (req.body) {
      for (const key in req.body) {
        if (typeof req.body[key] === 'string') {
          req.body[key] = xss(req.body[key]);
        }
      }
    }
    next();
  });

  // 8. URL validation
  securityMiddleware.push((req, res, next) => {
    const url = req.url;

    // Block suspicious URL patterns
    const suspiciousPatterns = [
      /\.\./,  // Directory traversal
      /\/etc\//,  // System files
      /\/proc\//,  // Process files
      /javascript:/i,  // JavaScript protocol
      /data:/i  // Data protocol
    ];

    for (const pattern of suspiciousPatterns) {
      if (pattern.test(url)) {
        return res.status(400).json({
          error: 'Invalid URL pattern detected'
        });
      }
    }
    next();
  });

  // 9. IP whitelist/blacklist
  securityMiddleware.push((req, res, next) => {
    const clientIp = req.ip || req.connection.remoteAddress;
    const whitelist = process.env.IP_WHITELIST?.split(',') || [];
    const blacklist = process.env.IP_BLACKLIST?.split(',') || [];

    // Check blacklist first
    if (blacklist.includes(clientIp)) {
      return res.status(403).json({
        error: 'Access denied from this IP address'
      });
    }

    // Check whitelist if it's not empty
    if (whitelist.length > 0 && !whitelist.includes(clientIp)) {
      return res.status(403).json({
        error: 'Access denied from this IP address'
      });
    }

    next();
  });

  // 10. API key validation for protected routes
  securityMiddleware.push((req, res, next) => {
    const protectedRoutes = process.env.PROTECTED_ROUTES?.split(',') || [];
    const isProtected = protectedRoutes.some(route => req.path.startsWith(route));

    if (isProtected) {
      const apiKey = req.get('X-API-Key') || req.get('Authorization')?.replace('Bearer ', '');
      const validKeys = process.env.API_KEYS?.split(',') || [];

      if (!apiKey || !validKeys.includes(apiKey)) {
        return res.status(401).json({
          error: 'Invalid or missing API key'
        });
      }
    }
    next();
  });

  // 11. Request ID tracking
  securityMiddleware.push((req, res, next) => {
    req.id = req.get('X-Request-ID') || `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    res.set('X-Request-ID', req.id);
    next();
  });

  // 12. Compression
  securityMiddleware.push(compression({
    filter: (req, res) => {
      if (req.headers['x-no-compression']) {
        return false;
      }
      return compression.filter(req, res);
    },
    level: 6,
    threshold: 1024
  }));

  return securityMiddleware;
};

// Security headers helper
const addSecurityHeaders = (req, res, next) => {
  // Additional custom security headers
  res.set({
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'X-XSS-Protection': '1; mode=block',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'Permissions-Policy': 'geolocation=(), microphone=(), camera=()',
    'X-Permitted-Cross-Domain-Policies': 'none',
    'X-Download-Options': 'noopen',
    'X-Content-Security-Policy': "default-src 'self'"
  });
  next();
};

// Input validation helper
const validateInput = (schema) => {
  return (req, res, next) => {
    const { error } = schema.validate(req.body);
    if (error) {
      return res.status(400).json({
        error: 'Validation failed',
        details: error.details.map(detail => ({
          field: detail.path.join('.'),
          message: detail.message
        }))
      });
    }
    next();
  };
};

// Rate limiting by user
const createUserRateLimiter = (windowMs, max) => {
  return rateLimit({
    windowMs,
    max,
    keyGenerator: (req) => {
      return req.user?.id || req.ip;
    },
    message: {
      error: 'Too many requests for this user, please try again later.'
    }
  });
};

module.exports = {
  createSecurityMiddleware,
  addSecurityHeaders,
  validateInput,
  createUserRateLimiter
};