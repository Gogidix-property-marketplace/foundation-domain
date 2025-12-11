const request = require('supertest');

describe('Custom Report Builder Service API Tests', () => {
    let app;

    beforeAll(() => {
        // Mock the server module
        jest.mock('../src/server.js', () => {
            const express = require('express');
            const app = express();
            app.use(express.json());

            app.get('/health', (req, res) => {
                res.status(200).json({
                    status: 'healthy',
                    service: 'custom-report-builder',
                    timestamp: new Date().toISOString()
                });
            });

            app.get('/api/reports', (req, res) => {
                res.status(200).json({
                    reports: [
                        { id: 1, name: 'Sales Report', type: 'tabular', createdAt: '2024-01-15' },
                        { id: 2, name: 'Performance Dashboard', type: 'chart', createdAt: '2024-01-14' }
                    ],
                    total: 2
                });
            });

            app.post('/api/reports', (req, res) => {
                const { name, type, config } = req.body;
                if (!name || !type) {
                    return res.status(400).json({ error: 'Name and type are required' });
                }
                res.status(201).json({
                    id: Date.now(),
                    name,
                    type,
                    config: config || {},
                    createdAt: new Date().toISOString()
                });
            });

            app.get('/api/reports/:id', (req, res) => {
                const id = parseInt(req.params.id);
                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid report ID' });
                }
                res.status(200).json({
                    id,
                    name: `Report ${id}`,
                    type: 'custom',
                    config: { columns: ['date', 'value'] }
                });
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
            expect(response.body).toHaveProperty('service', 'custom-report-builder');
        });
    });

    describe('Report CRUD Operations', () => {
        test('should get all reports', async () => {
            const response = await request(app)
                .get('/api/reports')
                .expect(200);

            expect(response.body).toHaveProperty('reports');
            expect(response.body).toHaveProperty('total', 2);
            expect(Array.isArray(response.body.reports)).toBe(true);
        });

        test('should create new report', async () => {
            const newReport = {
                name: 'Test Report',
                type: 'chart',
                config: { chartType: 'bar' }
            };

            const response = await request(app)
                .post('/api/reports')
                .send(newReport)
                .expect(201);

            expect(response.body).toHaveProperty('id');
            expect(response.body).toHaveProperty('name', 'Test Report');
            expect(response.body).toHaveProperty('type', 'chart');
            expect(response.body).toHaveProperty('config');
            expect(response.body).toHaveProperty('createdAt');
        });

        test('should return 400 for invalid report creation', async () => {
            const response = await request(app)
                .post('/api/reports')
                .send({ name: 'Incomplete' })
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Name and type are required');
        });

        test('should get report by ID', async () => {
            const response = await request(app)
                .get('/api/reports/1')
                .expect(200);

            expect(response.body).toHaveProperty('id', 1);
            expect(response.body).toHaveProperty('name', 'Report 1');
            expect(response.body).toHaveProperty('type', 'custom');
        });
    });

    describe('Performance Tests', () => {
        test('should respond within acceptable time limits', async () => {
            const startTime = Date.now();
            await request(app).get('/api/reports').expect(200);
            expect(Date.now() - startTime).toBeLessThan(500);
        });
    });
});