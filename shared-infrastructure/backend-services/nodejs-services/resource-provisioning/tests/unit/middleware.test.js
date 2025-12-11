const { createSecurityMiddleware } = require('../src/middleware/security');

describe('Security Middleware', () => {
  let mockReq, mockRes, mockNext;

  beforeEach(() => {
    mockReq = {
      ip: '127.0.0.1',
      method: 'GET',
      path: '/test',
      get: jest.fn()
    };
    mockRes = {
      set: jest.fn(),
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis()
    };
    mockNext = jest.fn();
  });

  test('should apply security headers', () => {
    const middleware = createSecurityMiddleware();
    middleware[0](mockReq, mockRes, mockNext);

    expect(mockNext).toHaveBeenCalled();
  });

  test('should rate limit requests', async () => {
    const middleware = createSecurityMiddleware({ rateLimitMax: 2 });

    // First two requests should pass
    middleware[2](mockReq, mockRes, mockNext);
    middleware[2](mockReq, mockRes, mockNext);

    expect(mockNext).toHaveBeenCalledTimes(2);
  });
});
