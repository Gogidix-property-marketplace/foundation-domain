const logger = require('../utils/logger');

/**
 * Custom error classes
 */
class AppError extends Error {
  constructor(message, statusCode, isOperational = true, stack = '') {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = isOperational;
    this.stack = stack;
    Error.captureStackTrace(this, this.constructor);
  }
}

class ValidationError extends AppError {
  constructor(message, errors = []) {
    super(message, 400);
    this.errors = errors;
  }
}

class NotFoundError extends AppError {
  constructor(message = 'Resource not found') {
    super(message, 404);
  }
}

class UnauthorizedError extends AppError {
  constructor(message = 'Unauthorized') {
    super(message, 401);
  }
}

class ForbiddenError extends AppError {
  constructor(message = 'Forbidden') {
    super(message, 403);
  }
}

class ConflictError extends AppError {
  constructor(message = 'Conflict') {
    super(message, 409);
  }
}

class TooManyRequestsError extends AppError {
  constructor(message = 'Too many requests') {
    super(message, 429);
  }
}

class ServiceUnavailableError extends AppError {
  constructor(message = 'Service temporarily unavailable') {
    super(message, 503);
  }
}

/**
 * Error handling middleware
 * @param {Error} err - Error object
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
function errorHandler(err, req, res, next) {
  let error = { ...err };

  // Handle specific error types
  if (err.name === 'ValidationError') {
    error = {
      name: 'ValidationError',
      message: err.message,
      errors: err.errors
    };
  } else if (err.name === 'CastError') {
    error = {
      name: 'CastError',
      message: 'Invalid data format',
      path: err.path
    };
  } else if (err.code === 11000) {
    error = {
      name: 'DuplicateError',
      message: 'Duplicate field value',
      field: err.keyValue
    };
  } else if (err.name === 'JsonWebTokenError') {
    error = {
      name: 'JsonWebTokenError',
      message: 'Invalid token'
    };
  } else if (err.name === 'TokenExpiredError') {
    error = {
      name: 'TokenExpiredError',
      message: 'Token expired'
    };
  } else if (err.code === 'LIMIT_FILE_SIZE') {
    error = {
      name: 'FileSizeError',
      message: 'File too large'
    };
  } else if (err.code === 'EACCES' || err.code === 'EPERM') {
    error = {
      name: 'FilePermissionError',
      message: 'File permission denied'
    };
  } else if (err.name === 'SyntaxError' && err.status === 400 && 'body' in err) {
    error = {
      name: 'InvalidJSON',
      message: 'Invalid JSON in request body'
    };
  } else if (!error.isOperational) {
    error = {
      name: 'InternalServerError',
      message: 'Internal server error',
      stack: err.stack
    };
  }

  // Log the error
  logger.error('Error:', {
    message: error.message,
    stack: error.stack,
    url: req.originalUrl,
    method: req.method,
    ip: req.ip,
    userAgent: req.get('User-Agent'),
    body: req.body,
    params: req.params,
    query: req.query
  });

  // Determine status code
  const statusCode = error.statusCode || 500;

  // Prepare error response
  const errorResponse = {
    error: error.name || 'InternalServerError',
    message: error.message || 'Internal server error',
    timestamp: new Date().toISOString(),
    path: req.originalUrl,
    method: req.method
  };

  // Include additional error details in development
  if (process.env.NODE_ENV === 'development') {
    errorResponse.stack = error.stack;
    errorResponse.details = {
      errors: error.errors,
      path: error.path,
      field: error.field,
      key: error.key
    };
  }

  // Remove sensitive information from production response
  if (process.env.NODE_ENV === 'production') {
    delete errorResponse.stack;
    delete errorResponse.details;
  }

  // Send error response
  res.status(statusCode).json(errorResponse);
}

/**
 * 404 Not Found handler
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
function notFoundHandler(req, res, next) {
  const error = new NotFoundError(`Route ${req.originalUrl} not found`);
  next(error);
}

/**
 * Async error wrapper
 * @param {Function} fn - Async function to wrap
 * @returns {Function} Express middleware
 */
function asyncHandler(fn) {
  return (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

/**
 * Handle uncaught exceptions
 */
process.on('uncaughtException', (err) => {
  logger.error('Uncaught Exception:', err);
  process.exit(1);
});

/**
 * Handle unhandled promise rejections
 */
process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
  process.exit(1);
});

module.exports = {
  AppError,
  ValidationError,
  NotFoundError,
  UnauthorizedError,
  ForbiddenError,
  ConflictError,
  TooManyRequestsError,
  ServiceUnavailableError,
  errorHandler,
  notFoundHandler,
  asyncHandler
};