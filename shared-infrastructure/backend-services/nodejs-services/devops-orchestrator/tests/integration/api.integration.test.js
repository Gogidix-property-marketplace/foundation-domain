const request = require('supertest');
const app = require('../src/server');

describe('devops-orchestrator API Integration Tests', () => {
  test('should handle complete request lifecycle', async () => {
    const response = await request(app)
      .get('/api/v1')
      .set('Accept', 'application/json')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toBeInstanceOf(Object);
  });

  test('should handle concurrent requests', async () => {
    const promises = Array(10).fill().map(() =>
      request(app).get('/health')
    );

    const responses = await Promise.all(promises);
    responses.forEach(res => {
      expect(res.status).toBe(200);
    });
  });

  test('should handle large payload', async () => {
    const largePayload = 'x'.repeat(10000);

    const response = await request(app)
      .post('/api/v1/test')
      .send({ data: largePayload })
      .set('Content-Type', 'application/json');

    // Should not crash with large payload
    expect([200, 404, 400]).toContain(response.status);
  });
});
