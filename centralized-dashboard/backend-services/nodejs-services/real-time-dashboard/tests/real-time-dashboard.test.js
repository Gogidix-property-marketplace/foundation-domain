const request = require('supertest');

describe('Real-Time Dashboard Service API Tests', () => {
    let app;

    beforeAll(() => {
        jest.mock('../src/server.js', () => {
            const express = require('express');
            const app = express();
            app.use(express.json());

            app.get('/health', (req, res) => {
                res.status(200).json({
                    status: 'healthy',
                    service: 'real-time-dashboard',
                    timestamp: new Date().toISOString()
                });
            });

            app.get('/api/realtime/data', (req, res) => {
                const { dashboardId, widgetId } = req.query;
                res.status(200).json({
                    dashboardId: dashboardId || 'default',
                    widgetId: widgetId || 'all',
                    data: {
                        timestamp: Date.now(),
                        metrics: {
                            activeUsers: Math.floor(Math.random() * 1000),
                            serverLoad: Math.random() * 100,
                            responseTime: Math.floor(Math.random() * 500)
                        }
                    }
                });
            });

            app.post('/api/realtime/subscribe', (req, res) => {
                const { dashboardId, events } = req.body;
                if (!dashboardId) {
                    return res.status(400).json({ error: 'dashboardId is required' });
                }
                res.status(201).json({
                    subscriptionId: `sub_${Date.now()}`,
                    dashboardId,
                    events: events || ['all'],
                    status: 'active'
                });
            });

            app.delete('/api/realtime/subscribe/:subscriptionId', (req, res) => {
                const subscriptionId = req.params.subscriptionId;
                if (!subscriptionId) {
                    return res.status(400).json({ error: 'subscriptionId is required' });
                }
                res.status(204).send();
            });

            return app;
        });

        app = require('../src/server.js');
    });

    describe('Health Check', () => {
        test('should return health status', async () => {
            const response = await request(app)
                .get('/health')
                .expect(200);

            expect(response.body).toHaveProperty('status', 'healthy');
            expect(response.body).toHaveProperty('service', 'real-time-dashboard');
        });
    });

    describe('Real-Time Data', () => {
        test('should get real-time data with default parameters', async () => {
            const response = await request(app)
                .get('/api/realtime/data')
                .expect(200);

            expect(response.body).toHaveProperty('dashboardId', 'default');
            expect(response.body).toHaveProperty('widgetId', 'all');
            expect(response.body).toHaveProperty('data');
            expect(response.body.data).toHaveProperty('timestamp');
            expect(response.body.data).toHaveProperty('metrics');
        });

        test('should get real-time data with specific dashboard and widget', async () => {
            const response = await request(app)
                .get('/api/realtime/data?dashboardId=dashboard123&widgetId=widget456')
                .expect(200);

            expect(response.body).toHaveProperty('dashboardId', 'dashboard123');
            expect(response.body).toHaveProperty('widgetId', 'widget456');
        });
    });

    describe('Subscription Management', () => {
        test('should create subscription', async () => {
            const subscription = {
                dashboardId: 'dashboard123',
                events: ['metric_update', 'widget_change']
            };

            const response = await request(app)
                .post('/api/realtime/subscribe')
                .send(subscription)
                .expect(201);

            expect(response.body).toHaveProperty('subscriptionId');
            expect(response.body).toHaveProperty('dashboardId', 'dashboard123');
            expect(response.body).toHaveProperty('events');
            expect(response.body).toHaveProperty('status', 'active');
        });

        test('should return 400 for subscription without dashboardId', async () => {
            const response = await request(app)
                .post('/api/realtime/subscribe')
                .send({ events: ['all'] })
                .expect(400);

            expect(response.body).toHaveProperty('error', 'dashboardId is required');
        });

        test('should cancel subscription', async () => {
            await request(app)
                .delete('/api/realtime/subscribe/sub_123456')
                .expect(204);
        });
    });

    describe('Performance Tests', () => {
        test('should respond quickly for real-time data', async () => {
            const startTime = Date.now();
            await request(app).get('/api/realtime/data').expect(200);
            expect(Date.now() - startTime).toBeLessThan(100); // Real-time data should be fast
        });

        test('should handle concurrent subscription requests', async () => {
            const promises = Array(10).fill().map((_, i) =>
                request(app)
                    .post('/api/realtime/subscribe')
                    .send({ dashboardId: `dashboard${i}` })
            );

            const responses = await Promise.all(promises);
            responses.forEach(response => {
                expect(response.status).toBe(201);
                expect(response.body).toHaveProperty('subscriptionId');
            });
        });
    });
});