const request = require('supertest');
const app = require('../src/server');

describe('cost-optimizer Server', () => {
  test('should return 404 for unknown routes', async () => {
    const response = await request(app)
      .get('/unknown-route')
      .expect(404);

    expect(response.body).toHaveProperty('error');
  });

  test('should handle CORS preflight requests', async () => {
    const response = await request(app)
      .options('/api/v1')
      .expect(204);
  });
});
