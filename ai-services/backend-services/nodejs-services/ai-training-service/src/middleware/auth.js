const jwt = require('jsonwebtoken');
const logger = require('../utils/logger');

const auth = {
  /**
   * Authentication middleware
   * Verifies JWT token and attaches user to request
   */
  authenticate: (req, res, next) => {
    try {
      const authHeader = req.headers.authorization;

      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          error: 'Access denied. No token provided.',
          code: 'TOKEN_MISSING'
        });
      }

      const token = authHeader.substring(7); // Remove 'Bearer ' prefix

      if (!token) {
        return res.status(401).json({
          success: false,
          error: 'Access denied. Token not found.',
          code: 'TOKEN_NOT_FOUND'
        });
      }

      // Verify token
      const decoded = jwt.verify(token, process.env.JWT_SECRET || 'fallback-secret-change-in-production');

      // Attach user to request
      req.user = {
        id: decoded.sub || decoded.userId,
        email: decoded.email,
        role: decoded.role || 'user',
        permissions: decoded.permissions || []
      };

      // Log authentication success
      logger.debug('User authenticated:', {
        userId: req.user.id,
        email: req.user.email,
        role: req.user.role,
        ip: req.ip,
        userAgent: req.get('User-Agent')
      });

      next();
    } catch (error) {
      logger.error('Authentication error:', error);

      if (error.name === 'JsonWebTokenError') {
        return res.status(401).json({
          success: false,
          error: 'Invalid token.',
          code: 'TOKEN_INVALID'
        });
      }

      if (error.name === 'TokenExpiredError') {
        return res.status(401).json({
          success: false,
          error: 'Token expired.',
          code: 'TOKEN_EXPIRED'
        });
      }

      if (error.name === 'NotBeforeError') {
        return res.status(401).json({
          success: false,
          error: 'Token not active.',
          code: 'TOKEN_NOT_ACTIVE'
        });
      }

      res.status(500).json({
        success: false,
        error: 'Authentication server error.',
        code: 'AUTH_SERVER_ERROR'
      });
    }
  },

  /**
   * Authorization middleware factory
   * Creates middleware that checks if user has required role
   */
  requireRole: (roles) => {
    if (typeof roles === 'string') {
      roles = [roles];
    }

    return (req, res, next) => {
      if (!req.user) {
        return res.status(401).json({
          success: false,
          error: 'Authentication required.',
          code: 'AUTH_REQUIRED'
        });
      }

      if (!roles.includes(req.user.role)) {
        return res.status(403).json({
          success: false,
          error: 'Insufficient permissions.',
          code: 'INSUFFICIENT_PERMISSIONS',
          required: roles,
          current: req.user.role
        });
      }

      logger.debug('Role check passed:', {
        userId: req.user.id,
        role: req.user.role,
        required: roles
      });

      next();
    };
  },

  /**
   * Permission check middleware factory
   * Creates middleware that checks if user has required permissions
   */
  requirePermission: (permissions) => {
    if (typeof permissions === 'string') {
      permissions = [permissions];
    }

    return (req, res, next) => {
      if (!req.user) {
        return res.status(401).json({
          success: false,
          error: 'Authentication required.',
          code: 'AUTH_REQUIRED'
        });
      }

      // Admin role has all permissions
      if (req.user.role === 'admin') {
        return next();
      }

      const userPermissions = req.user.permissions || [];
      const hasAllPermissions = permissions.every(permission =>
        userPermissions.includes(permission)
      );

      if (!hasAllPermissions) {
        return res.status(403).json({
          success: false,
          error: 'Insufficient permissions.',
          code: 'INSUFFICIENT_PERMISSIONS',
          required: permissions,
          current: userPermissions
        });
      }

      logger.debug('Permission check passed:', {
        userId: req.user.id,
        permissions: userPermissions,
        required: permissions
      });

      next();
    };
  },

  /**
   * Optional authentication middleware
   * Attaches user if token is present but doesn't fail if not
   */
  optionalAuth: (req, res, next) => {
    try {
      const authHeader = req.headers.authorization;

      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return next();
      }

      const token = authHeader.substring(7);

      if (!token) {
        return next();
      }

      // Verify token
      const decoded = jwt.verify(token, process.env.JWT_SECRET || 'fallback-secret-change-in-production');

      // Attach user to request
      req.user = {
        id: decoded.sub || decoded.userId,
        email: decoded.email,
        role: decoded.role || 'user',
        permissions: decoded.permissions || []
      };

      next();
    } catch (error) {
      // Log error but don't fail the request
      logger.debug('Optional authentication failed:', error.message);
      next();
    }
  },

  /**
   * API key authentication middleware
   * Alternative authentication method for service-to-service communication
   */
  authenticateApiKey: (req, res, next) => {
    try {
      const apiKey = req.headers['x-api-key'];

      if (!apiKey) {
        return res.status(401).json({
          success: false,
          error: 'API key required.',
          code: 'API_KEY_MISSING'
        });
      }

      // In a real implementation, verify API key against database
      const validApiKeys = (process.env.VALID_API_KEYS || '').split(',');

      if (!validApiKeys.includes(apiKey)) {
        return res.status(401).json({
          success: false,
          error: 'Invalid API key.',
          code: 'API_KEY_INVALID'
        });
      }

      // Attach service info to request
      req.service = {
        apiKey,
        type: 'api_key',
        authenticated: true
      };

      logger.debug('API key authenticated:', {
        apiKey: apiKey.substring(0, 8) + '...',
        ip: req.ip
      });

      next();
    } catch (error) {
      logger.error('API key authentication error:', error);
      res.status(500).json({
        success: false,
        error: 'Authentication server error.',
        code: 'AUTH_SERVER_ERROR'
      });
    }
  },

  /**
   * Service account authentication middleware
   * For internal service communication
   */
  authenticateService: (req, res, next) => {
    try {
      const serviceToken = req.headers['x-service-token'];

      if (!serviceToken) {
        return res.status(401).json({
          success: false,
          error: 'Service token required.',
          code: 'SERVICE_TOKEN_MISSING'
        });
      }

      // Verify service token
      const decoded = jwt.verify(serviceToken, process.env.SERVICE_JWT_SECRET || process.env.JWT_SECRET);

      // Attach service to request
      req.service = {
        id: decoded.serviceId,
        name: decoded.serviceName,
        type: 'service_account',
        permissions: decoded.permissions || []
      };

      logger.debug('Service authenticated:', {
        serviceId: req.service.id,
        serviceName: req.service.name,
        ip: req.ip
      });

      next();
    } catch (error) {
      logger.error('Service authentication error:', error);
      res.status(401).json({
        success: false,
        error: 'Invalid service token.',
        code: 'SERVICE_TOKEN_INVALID'
      });
    }
  },

  /**
   * Rate limiting check based on user tier
   */
  checkRateLimit: (limit, window) => {
    const requests = new Map();

    return (req, res, next) => {
      if (!req.user) {
        return next();
      }

      const key = req.user.id;
      const now = Date.now();
      const windowMs = window * 1000;

      // Clean old entries
      if (requests.has(key)) {
        const userRequests = requests.get(key).filter(time => now - time < windowMs);
        requests.set(key, userRequests);
      } else {
        requests.set(key, []);
      }

      // Check limit
      if (requests.get(key).length >= limit) {
        return res.status(429).json({
          success: false,
          error: 'Rate limit exceeded.',
          code: 'RATE_LIMIT_EXCEEDED',
          limit,
          window,
          retryAfter: Math.ceil((requests.get(key)[0] + windowMs - now) / 1000)
        });
      }

      // Add current request
      requests.get(key).push(now);

      // Set rate limit headers
      res.set({
        'X-RateLimit-Limit': limit,
        'X-RateLimit-Remaining': Math.max(0, limit - requests.get(key).length),
        'X-RateLimit-Reset': new Date(now + windowMs).toISOString()
      });

      next();
    };
  }
};

module.exports = auth;