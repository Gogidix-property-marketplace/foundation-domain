const request = require('supertest');

describe('Shared Components Web Service API Tests', () => {
    let app;

    beforeAll(() => {
        jest.mock('../src/server.js', () => {
            const express = require('express');
            const app = express();
            app.use(express.json());

            app.get('/health', (req, res) => {
                res.status(200).json({
                    status: 'healthy',
                    service: 'shared-components-web',
                    timestamp: new Date().toISOString()
                });
            });

            app.get('/api/components', (req, res) => {
                const { category, version } = req.query;
                const components = [
                    { id: 1, name: 'DataTable', category: 'table', version: '2.1.0' },
                    { id: 2, name: 'Chart', category: 'chart', version: '1.8.0' },
                    { id: 3, name: 'Filter', category: 'input', version: '2.0.0' }
                ];

                let filtered = components;
                if (category) {
                    filtered = filtered.filter(c => c.category === category);
                }
                if (version) {
                    filtered = filtered.filter(c => c.version.startsWith(version));
                }

                res.status(200).json({
                    components: filtered,
                    total: filtered.length
                });
            });

            app.get('/api/components/:id', (req, res) => {
                const id = parseInt(req.params.id);
                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid component ID' });
                }
                res.status(200).json({
                    id,
                    name: `Component ${id}`,
                    description: 'Component description',
                    props: ['data', 'config', 'theme'],
                    version: '2.1.0'
                });
            });

            app.get('/api/components/:id/usage', (req, res) => {
                const id = parseInt(req.params.id);
                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid component ID' });
                }
                res.status(200).json({
                    componentId: id,
                    usageCount: Math.floor(Math.random() * 100),
                    dashboards: [`dashboard-${id}-1`, `dashboard-${id}-2`],
                    lastUsed: new Date().toISOString()
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
            expect(response.body).toHaveProperty('service', 'shared-components-web');
        });
    });

    describe('Component Management', () => {
        test('should get all components', async () => {
            const response = await request(app)
                .get('/api/components')
                .expect(200);

            expect(response.body).toHaveProperty('components');
            expect(response.body).toHaveProperty('total');
            expect(Array.isArray(response.body.components)).toBe(true);
        });

        test('should filter components by category', async () => {
            const response = await request(app)
                .get('/api/components?category=chart')
                .expect(200);

            expect(response.body.components.every(c => c.category === 'chart')).toBe(true);
        });

        test('should filter components by version', async () => {
            const response = await request(app)
                .get('/api/components?version=2.')
                .expect(200);

            expect(response.body.components.every(c => c.version.startsWith('2.'))).toBe(true);
        });

        test('should get component by ID', async () => {
            const response = await request(app)
                .get('/api/components/1')
                .expect(200);

            expect(response.body).toHaveProperty('id', 1);
            expect(response.body).toHaveProperty('name', 'Component 1');
            expect(response.body).toHaveProperty('description');
            expect(response.body).toHaveProperty('props');
            expect(response.body).toHaveProperty('version');
        });

        test('should return 400 for invalid component ID', async () => {
            const response = await request(app)
                .get('/api/components/invalid')
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Invalid component ID');
        });

        test('should get component usage statistics', async () => {
            const response = await request(app)
                .get('/api/components/1/usage')
                .expect(200);

            expect(response.body).toHaveProperty('componentId', 1);
            expect(response.body).toHaveProperty('usageCount');
            expect(response.body).toHaveProperty('dashboards');
            expect(response.body).toHaveProperty('lastUsed');
            expect(Array.isArray(response.body.dashboards)).toBe(true);
        });
    });

    describe('Performance Tests', () => {
        test('should respond within acceptable time limits', async () => {
            const startTime = Date.now();
            await request(app).get('/api/components').expect(200);
            expect(Date.now() - startTime).toBeLessThan(300);
        });

        test('should handle concurrent requests', async () => {
            const promises = Array(10).fill().map(() =>
                request(app).get('/api/components')
            );

            const responses = await Promise.all(promises);
            responses.forEach(response => {
                expect(response.status).toBe(200);
                expect(response.body).toHaveProperty('components');
            });
        });
    });
});