const request = require('supertest');
const express = require('express');
const analyticsController = require('../../src/controllers/analyticsController');
const { AnalyticsEvent, ModelPerformance, RealTimeMetric } = require('../../src/models/Analytics');
const auth = require('../../src/middleware/auth');

// Mock auth middleware
jest.mock('../../src/middleware/auth', () => ({
  authenticate: (req, res, next) => {
    req.user = {
      id: 'test-user-id',
      email: 'test@example.com',
      role: 'admin',
      permissions: ['read:analytics', 'write:analytics']
    };
    next();
  }
}));

const app = express();
app.use(express.json());

// Import routes
const analyticsRoutes = require('../../src/routes/analytics');
app.use('/api/v1/analytics', analyticsRoutes);

describe('Analytics Controller', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('GET /api/v1/analytics/events', () => {
    it('should return analytics events successfully', async () => {
      // Create test data
      await AnalyticsEvent.create({
        eventType: 'model_prediction',
        serviceName: 'dashboard',
        userId: 'test-user-id',
        timestamp: new Date(),
        properties: { model: 'test-model' }
      });

      const response = await request(app)
        .get('/api/v1/analytics/events')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data).toHaveProperty('events');
      expect(response.body.data.events).toHaveLength(1);
      expect(response.body.data.events[0].eventType).toBe('model_prediction');
    });

    it('should filter events by service name', async () => {
      // Create test data
      await AnalyticsEvent.create([
        {
          eventType: 'model_prediction',
          serviceName: 'dashboard',
          userId: 'test-user-id',
          timestamp: new Date()
        },
        {
          eventType: 'model_training',
          serviceName: 'training',
          userId: 'test-user-id',
          timestamp: new Date()
        }
      ]);

      const response = await request(app)
        .get('/api/v1/analytics/events?serviceName=dashboard')
        .expect(200);

      expect(response.body.data.events).toHaveLength(1);
      expect(response.body.data.events[0].serviceName).toBe('dashboard');
    });

    it('should limit number of events returned', async () => {
      // Create multiple test events
      const events = Array.from({ length: 25 }, (_, i) => ({
        eventType: 'model_prediction',
        serviceName: 'dashboard',
        userId: 'test-user-id',
        timestamp: new Date()
      }));

      await AnalyticsEvent.insertMany(events);

      const response = await request(app)
        .get('/api/v1/analytics/events?limit=20')
        .expect(200);

      expect(response.body.data.events).toHaveLength(20);
    });
  });

  describe('POST /api/v1/analytics/events', () => {
    it('should create a new analytics event', async () => {
      const eventData = {
        eventType: 'model_prediction',
        serviceName: 'dashboard',
        userId: 'test-user-id',
        properties: {
          modelId: 'model-123',
          prediction: 'positive',
          confidence: 0.95
        }
      };

      const response = await request(app)
        .post('/api/v1/analytics/events')
        .send(eventData)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.event).toHaveProperty('_id');
      expect(response.body.data.event.eventType).toBe('model_prediction');
      expect(response.body.data.event.properties.modelId).toBe('model-123');
    });

    it('should validate required fields', async () => {
      const invalidEventData = {
        serviceName: 'dashboard'
        // Missing required eventType
      };

      const response = await request(app)
        .post('/api/v1/analytics/events')
        .send(invalidEventData)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toContain('required');
    });
  });

  describe('GET /api/v1/analytics/metrics', () => {
    it('should return real-time metrics', async () => {
      // Create test metrics
      await RealTimeMetric.create({
        serviceName: 'dashboard',
        metricName: 'response_time',
        metricValue: 120,
        timestamp: new Date()
      });

      const response = await request(app)
        .get('/api/v1/analytics/metrics')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data).toHaveProperty('metrics');
    });

    it('should filter metrics by service', async () => {
      await RealTimeMetric.create([
        {
          serviceName: 'dashboard',
          metricName: 'response_time',
          metricValue: 120,
          timestamp: new Date()
        },
        {
          serviceName: 'training',
          metricName: 'accuracy',
          metricValue: 0.95,
          timestamp: new Date()
        }
      ]);

      const response = await request(app)
        .get('/api/v1/analytics/metrics?service=dashboard')
        .expect(200);

      const metrics = response.body.data.metrics;
      metrics.forEach(metric => {
        expect(metric.serviceName).toBe('dashboard');
      });
    });
  });

  describe('POST /api/v1/analytics/metrics', () => {
    it('should create a new metric', async () => {
      const metricData = {
        serviceName: 'dashboard',
        metricName: 'cpu_usage',
        metricValue: 65.5,
        unit: 'percent',
        labels: {
          host: 'server-1',
          environment: 'production'
        }
      };

      const response = await request(app)
        .post('/api/v1/analytics/metrics')
        .send(metricData)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.metric.serviceName).toBe('dashboard');
      expect(response.body.data.metric.metricValue).toBe(65.5);
    });
  });

  describe('GET /api/v1/analytics/models/performance', () => {
    it('should return model performance data', async () => {
      await ModelPerformance.create({
        modelId: 'model-123',
        serviceName: 'dashboard',
        metrics: {
          accuracy: 0.95,
          precision: 0.93,
          recall: 0.91,
          f1Score: 0.92
        },
        timestamp: new Date()
      });

      const response = await request(app)
        .get('/api/v1/analytics/models/performance')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data).toHaveProperty('performances');
    });
  });

  describe('Error Handling', () => {
    it('should handle invalid query parameters', async () => {
      const response = await request(app)
        .get('/api/v1/analytics/events?limit=invalid')
        .expect(400);

      expect(response.body.success).toBe(false);
    });

    it('should handle database errors gracefully', async () => {
      // Mock database error
      jest.spyOn(AnalyticsEvent, 'find').mockImplementationOnce(() => {
        throw new Error('Database connection failed');
      });

      const response = await request(app)
        .get('/api/v1/analytics/events')
        .expect(500);

      expect(response.body.success).toBe(false);
    });
  });
});