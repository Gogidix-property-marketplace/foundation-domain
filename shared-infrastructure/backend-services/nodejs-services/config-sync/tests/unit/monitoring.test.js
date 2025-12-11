const { createMonitoringMiddleware } = require('../src/middleware/monitoring');

describe('Monitoring Middleware', () => {
  let mockReq, mockRes, mockNext;

  beforeEach(() => {
    mockReq = {
      method: 'GET',
      originalUrl: '/test',
      ip: '127.0.0.1',
      get: jest.fn()
    };
    mockRes = {
      on: jest.fn(),
      set: jest.fn()
    };
    mockNext = jest.fn();
  });

  test('should initialize monitoring', () => {
    const monitoring = createMonitoringMiddleware('config-sync');

    expect(monitoring.logger).toBeDefined();
    expect(monitoring.metrics).toBeDefined();
  });

  test('should track request metrics', () => {
    const monitoring = createMonitoringMiddleware('config-sync');
    monitoring.requestMetrics(mockReq, mockRes, mockNext);

    expect(mockNext).toHaveBeenCalled();
  });
});
