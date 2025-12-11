const request = require('supertest');
const app = require('../../src/app');

describe('Smoke Tests - AI Dashboard Web Service', () => {
  describe('Basic Service Health', () => {
    it('should start the application successfully', async () => {
      // Test that the app can be instantiated without errors
      expect(app).toBeDefined();
    });

    it('should respond to health check endpoint', async () => {
      const response = await request(app)
        .get('/health')
        .expect(200);

      expect(response.body).toHaveProperty('status');
      expect(response.body).toHaveProperty('timestamp');
      expect(response.body).toHaveProperty('uptime');
      expect(typeof response.body.uptime).toBe('number');
    });
  });

  describe('API Endpoint Availability', () => {
    it('should have analytics endpoints available', async () => {
      // Test OPTIONS request to check endpoint availability
      await request(app)
        .options('/api/v1/analytics/events')
        .expect(200);
    });

    it('should have dashboard endpoints available', async () => {
      await request(app)
        .options('/api/v1/dashboard/overview')
        .expect(200);
    });

    it('should have reports endpoints available', async () => {
      await request(app)
        .options('/api/v1/reports')
        .expect(200);
    });
  });

  describe('Database Connectivity', () => {
    it('should connect to MongoDB successfully', async () => {
      // This is tested implicitly through successful operations
      // but we can also test the health check that includes DB status
      const response = await request(app)
        .get('/health')
        .expect(200);

      // Health check should include database status
      if (response.body.database) {
        expect(response.body.database.status).toBe('connected');
      }
    });
  });

  describe('Essential Middleware', () => {
    it('should have CORS enabled', async () => {
      const response = await request(app)
        .options('/api/v1/analytics/events')
        .expect(200);

      expect(response.headers).toHaveProperty('access-control-allow-origin');
    });

    it('should have security headers', async () => {
      const response = await request(app)
        .get('/health')
        .expect(200);

      // Check for common security headers
      expect(response.headers).toHaveProperty('x-content-type-options');
      expect(response.headers).toHaveProperty('x-frame-options');
    });
  });

  describe('Logging Configuration', () => {
    it('should have Winston logger configured', async () => {
      const logger = require('../../src/utils/logger');
      expect(logger).toBeDefined();
      expect(typeof logger.info).toBe('function');
      expect(typeof logger.error).toBe('function');
    });
  });

  describe('Environment Configuration', () => {
    it('should have required environment variables', () => {
      expect(process.env.NODE_ENV).toBeDefined();
      expect(process.env.PORT || 3000).toBeDefined();
    });
  });

  describe('Critical Paths', () => {
    it('should handle authentication middleware without crashing', async () => {
      // Test that auth middleware doesn't crash the app
      await request(app)
        .get('/api/v1/analytics/events')
        .expect(401); // Should get 401, not 500
    });

    it('should handle error middleware gracefully', async () => {
      // Trigger an error that should be caught by error handler
      await request(app)
        .post('/api/v1/analytics/events')
        .send({})
        .expect(400); // Should get validation error, not 500
    });
  });

  describe('Memory Usage', () => {
    it('should not have excessive memory usage on startup', () => {
      const memUsage = process.memoryUsage();
      const heapUsedMB = memUsage.heapUsed / 1024 / 1024;

      // Should use less than 100MB on startup (adjust as needed)
      expect(heapUsedMB).toBeLessThan(100);
    });
  });

  describe('Service Dependencies', () => {
    it('should have Express.js properly configured', () => {
      expect(app).toBeDefined();
      expect(typeof app.listen).toBe('function');
      expect(typeof app.use).toBe('function');
    });

    it('should have MongoDB models properly loaded', () => {
      const { AnalyticsEvent, ModelPerformance, RealTimeMetric } = require('../../src/models/Analytics');
      expect(AnalyticsEvent).toBeDefined();
      expect(ModelPerformance).toBeDefined();
      expect(RealTimeMetric).toBeDefined();
    });
  });

  describe('Response Times', () => {
    it('should respond to health check within acceptable time', async () => {
      const start = Date.now();
      await request(app).get('/health').expect(200);
      const duration = Date.now() - start;

      // Health check should respond within 100ms
      expect(duration).toBeLessThan(100);
    });
  });
});