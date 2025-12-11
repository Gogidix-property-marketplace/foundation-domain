const request = require('supertest');
const app = require('../../src/app');

describe('AI Dashboard Web API E2E Tests', () => {
  let authToken;

  beforeAll(async () => {
    // Get auth token for testing
    const response = await request(app)
      .post('/api/v1/auth/login')
      .send({
        email: 'test@gogidix.com',
        password: 'test-password'
      });

    authToken = response.body.data.token;
  });

  describe('Health Endpoint', () => {
    it('should return health status', async () => {
      const response = await request(app)
        .get('/health')
        .expect(200);

      expect(response.body).toHaveProperty('status', 'healthy');
      expect(response.body).toHaveProperty('timestamp');
      expect(response.body).toHaveProperty('uptime');
    });
  });

  describe('Authentication', () => {
    it('should reject requests without token', async () => {
      await request(app)
        .get('/api/v1/analytics/events')
        .expect(401);
    });

    it('should accept requests with valid token', async () => {
      const response = await request(app)
        .get('/api/v1/analytics/events')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
    });
  });

  describe('Analytics API Workflow', () => {
    it('should complete full analytics workflow', async () => {
      // 1. Create an analytics event
      const eventResponse = await request(app)
        .post('/api/v1/analytics/events')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          eventType: 'model_prediction',
          serviceName: 'dashboard',
          userId: 'test-user',
          properties: {
            modelId: 'model-123',
            prediction: 'positive'
          }
        })
        .expect(201);

      const eventId = eventResponse.body.data.event._id;

      // 2. Create a metric
      await request(app)
        .post('/api/v1/analytics/metrics')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          serviceName: 'dashboard',
          metricName: 'response_time',
          metricValue: 150,
          unit: 'milliseconds'
        })
        .expect(201);

      // 3. Create model performance record
      await request(app)
        .post('/api/v1/analytics/models/performance')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          modelId: 'model-123',
          serviceName: 'dashboard',
          metrics: {
            accuracy: 0.95,
            precision: 0.93,
            recall: 0.91
          }
        })
        .expect(201);

      // 4. Retrieve all events and verify our event is there
      const eventsResponse = await request(app)
        .get('/api/v1/analytics/events')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      const createdEvent = eventsResponse.body.data.events.find(
        e => e._id === eventId
      );
      expect(createdEvent).toBeDefined();
      expect(createdEvent.eventType).toBe('model_prediction');

      // 5. Get metrics summary
      const metricsResponse = await request(app)
        .get('/api/v1/analytics/metrics/summary')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(metricsResponse.body.data).toHaveProperty('summary');
    });
  });

  describe('Dashboard API', () => {
    it('should return dashboard overview', async () => {
      const response = await request(app)
        .get('/api/v1/dashboard/overview')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data).toHaveProperty('totalModels');
      expect(response.body.data).toHaveProperty('activeTrainings');
      expect(response.body.data).toHaveProperty('predictionsToday');
      expect(response.body.data).toHaveProperty('systemHealth');
    });

    it('should return services status', async () => {
      const response = await request(app)
        .get('/api/v1/dashboard/services')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.data).toHaveProperty('services');
      expect(Array.isArray(response.body.data.services)).toBe(true);
    });
  });

  describe('Reports API', () => {
    it('should create a new report', async () => {
      const response = await request(app)
        .post('/api/v1/reports/generate')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          reportType: 'performance',
          timeRange: {
            start: '2025-11-29T00:00:00Z',
            end: '2025-11-30T00:00:00Z'
          },
          format: 'json'
        })
        .expect(201);

      expect(response.body.data).toHaveProperty('reportId');
      expect(response.body.data).toHaveProperty('status', 'generating');
    });

    it('should list available reports', async () => {
      const response = await request(app)
        .get('/api/v1/reports')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.data).toHaveProperty('reports');
      expect(Array.isArray(response.body.data.reports)).toBe(true);
    });
  });

  describe('Error Handling', () => {
    it('should handle 404 for unknown endpoints', async () => {
      await request(app)
        .get('/api/v1/unknown-endpoint')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(404);
    });

    it('should validate input data', async () => {
      await request(app)
        .post('/api/v1/analytics/events')
        .set('Authorization', `Bearer ${authToken}`)
        .send({})
        .expect(400);
    });

    it('should handle invalid JSON', async () => {
      await request(app)
        .post('/api/v1/analytics/events')
        .set('Authorization', `Bearer ${authToken}`)
        .set('Content-Type', 'application/json')
        .send('invalid json')
        .expect(400);
    });
  });

  describe('Rate Limiting', () => {
    it('should apply rate limits', async () => {
      const requests = Array.from({ length: 100 }, () =>
        request(app)
          .get('/api/v1/analytics/events')
          .set('Authorization', `Bearer ${authToken}`)
      );

      const responses = await Promise.allSettled(requests);
      const rateLimited = responses.filter(r =>
        r.status === 'fulfilled' && r.value.status === 429
      );

      expect(rateLimited.length).toBeGreaterThan(0);
    });
  });

  describe('CORS Headers', () => {
    it('should include CORS headers', async () => {
      const response = await request(app)
        .options('/api/v1/analytics/events')
        .expect(200);

      expect(response.headers).toHaveProperty('access-control-allow-origin');
    });
  });
});