const request = require('supertest');
const app = require('./index');

describe('API Gateway', () => {
  describe('GET /health', () => {
    it('should return health status', async () => {
      const response = await request(app)
        .get('/health')
        .expect(200);

      expect(response.body).toHaveProperty('status', 'healthy');
      expect(response.body).toHaveProperty('service', 'api-gateway');
    });
  });

  describe('GET /api/v1/gateway', () => {
    it('should return gateway info', async () => {
      const response = await request(app)
        .get('/api/v1/gateway')
        .expect(200);

      expect(response.body).toHaveProperty('message', 'Foundation Domain API Gateway');
      expect(response.body).toHaveProperty('version', '1.0.0');
    });
  });

  describe('GET /nonexistent', () => {
    it('should return 404 for non-existent routes', async () => {
      await request(app)
        .get('/nonexistent')
        .expect(404);
    });
  });
});