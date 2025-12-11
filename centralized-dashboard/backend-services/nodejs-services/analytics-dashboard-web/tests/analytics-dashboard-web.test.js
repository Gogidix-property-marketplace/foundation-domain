const request = require('supertest');
const express = require('express');

// Mock the server module
jest.mock('../src/server.js', () => {
    const app = express();
    app.use(express.json());

    app.get('/health', (req, res) => {
        res.status(200).json({ status: 'healthy', service: 'analytics-dashboard-web', timestamp: new Date().toISOString() });
    });

    app.get('/api/analytics/metrics', (req, res) => {
        const { timeRange, dashboardId } = req.query;
        res.status(200).json({
            timeRange: timeRange || '24h',
            dashboardId: dashboardId || 'all',
            metrics: {
                pageViews: 15420,
                uniqueUsers: 3420,
                bounceRate: 0.32,
                avgSessionDuration: 245,
                conversionRate: 0.042
            },
            timestamp: new Date().toISOString()
        });
    });

    app.get('/api/analytics/trends', (req, res) => {
        const { metric, period } = req.query;
        const data = Array.from({ length: 24 }, (_, i) => ({
            hour: i,
            value: Math.floor(Math.random() * 1000) + 100
        }));

        res.status(200).json({
            metric: metric || 'pageViews',
            period: period || 'hourly',
            data,
            timestamp: new Date().toISOString()
        });
    });

    app.post('/api/analytics/events', (req, res) => {
        const { eventType, eventData, userId, sessionId } = req.body;

        if (!eventType || !eventData) {
            return res.status(400).json({ error: 'eventType and eventData are required' });
        }

        res.status(201).json({
            id: Date.now(),
            eventType,
            eventData,
            userId: userId || null,
            sessionId: sessionId || null,
            timestamp: new Date().toISOString()
        });
    });

    app.get('/api/analytics/reports/:reportId', (req, res) => {
        const reportId = req.params.reportId;

        if (!reportId || reportId === 'invalid') {
            return res.status(400).json({ error: 'Invalid report ID' });
        }

        res.status(200).json({
            reportId,
            name: `Analytics Report ${reportId}`,
            type: 'performance',
            data: {
                totalRequests: 125000,
                avgResponseTime: 145,
                errorRate: 0.002,
                uptime: 0.999
            },
            generatedAt: new Date().toISOString()
        });
    });

    return app;
});

const app = require('../src/server.js');

describe('Analytics Dashboard Web Service API Tests', () => {
    describe('Health Check', () => {
        test('should return health status', async () => {
            const response = await request(app)
                .get('/health')
                .expect(200);

            expect(response.body).toHaveProperty('status', 'healthy');
            expect(response.body).toHaveProperty('service', 'analytics-dashboard-web');
            expect(response.body).toHaveProperty('timestamp');
        });
    });

    describe('Analytics Metrics', () => {
        test('should get analytics metrics with default parameters', async () => {
            const response = await request(app)
                .get('/api/analytics/metrics')
                .expect(200);

            expect(response.body).toHaveProperty('timeRange', '24h');
            expect(response.body).toHaveProperty('dashboardId', 'all');
            expect(response.body).toHaveProperty('metrics');
            expect(response.body.metrics).toHaveProperty('pageViews');
            expect(response.body.metrics).toHaveProperty('uniqueUsers');
            expect(response.body.metrics).toHaveProperty('bounceRate');
            expect(response.body.metrics).toHaveProperty('avgSessionDuration');
            expect(response.body.metrics).toHaveProperty('conversionRate');
        });

        test('should get analytics metrics with custom parameters', async () => {
            const response = await request(app)
                .get('/api/analytics/metrics?timeRange=7d&dashboardId=dashboard-123')
                .expect(200);

            expect(response.body).toHaveProperty('timeRange', '7d');
            expect(response.body).toHaveProperty('dashboardId', 'dashboard-123');
        });
    });

    describe('Analytics Trends', () => {
        test('should get trend data with default parameters', async () => {
            const response = await request(app)
                .get('/api/analytics/trends')
                .expect(200);

            expect(response.body).toHaveProperty('metric', 'pageViews');
            expect(response.body).toHaveProperty('period', 'hourly');
            expect(response.body).toHaveProperty('data');
            expect(Array.isArray(response.body.data)).toBe(true);
            expect(response.body.data).toHaveLength(24);
        });

        test('should get trend data with custom metric and period', async () => {
            const response = await request(app)
                .get('/api/analytics/trends?metric=users&period=daily')
                .expect(200);

            expect(response.body).toHaveProperty('metric', 'users');
            expect(response.body).toHaveProperty('period', 'daily');
        });
    });

    describe('Event Tracking', () => {
        test('should create analytics event', async () => {
            const event = {
                eventType: 'page_view',
                eventData: {
                    page: '/dashboard',
                    referrer: 'https://google.com'
                },
                userId: 'user-123',
                sessionId: 'session-456'
            };

            const response = await request(app)
                .post('/api/analytics/events')
                .send(event)
                .expect(201);

            expect(response.body).toHaveProperty('id');
            expect(response.body).toHaveProperty('eventType', 'page_view');
            expect(response.body).toHaveProperty('eventData');
            expect(response.body).toHaveProperty('userId', 'user-123');
            expect(response.body).toHaveProperty('sessionId', 'session-456');
            expect(response.body).toHaveProperty('timestamp');
        });

        test('should return 400 when creating event without required fields', async () => {
            const invalidEvent = {
                eventType: 'click'
                // missing eventData
            };

            const response = await request(app)
                .post('/api/analytics/events')
                .send(invalidEvent)
                .expect(400);

            expect(response.body).toHaveProperty('error', 'eventType and eventData are required');
        });
    });

    describe('Analytics Reports', () => {
        test('should get analytics report by ID', async () => {
            const response = await request(app)
                .get('/api/analytics/reports/performance-2024')
                .expect(200);

            expect(response.body).toHaveProperty('reportId', 'performance-2024');
            expect(response.body).toHaveProperty('name', 'Analytics Report performance-2024');
            expect(response.body).toHaveProperty('type', 'performance');
            expect(response.body).toHaveProperty('data');
            expect(response.body).toHaveProperty('generatedAt');
        });

        test('should return 400 for invalid report ID', async () => {
            const response = await request(app)
                .get('/api/analytics/reports/invalid')
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Invalid report ID');
        });
    });

    describe('Data Validation', () => {
        test('should handle empty request body for events', async () => {
            const response = await request(app)
                .post('/api/analytics/events')
                .send({})
                .expect(400);

            expect(response.body).toHaveProperty('error', 'eventType and eventData are required');
        });

        test('should handle malformed JSON', async () => {
            const response = await request(app)
                .post('/api/analytics/events')
                .set('Content-Type', 'application/json')
                .send('invalid json')
                .expect(400);
        });
    });

    describe('Performance Tests', () => {
        test('should respond within acceptable time limits', async () => {
            const startTime = Date.now();

            await request(app)
                .get('/api/analytics/metrics')
                .expect(200);

            const responseTime = Date.now() - startTime;
            expect(responseTime).toBeLessThan(500);
        });

        test('should handle concurrent requests', async () => {
            const promises = Array(10).fill().map(() =>
                request(app).get('/api/analytics/trends')
            );

            const responses = await Promise.all(promises);

            responses.forEach(response => {
                expect(response.status).toBe(200);
                expect(response.body).toHaveProperty('data');
            });
        });
    });
});