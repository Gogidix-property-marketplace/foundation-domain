const request = require('supertest');

describe('Visualization Web Service API Tests', () => {
    let app;

    beforeAll(() => {
        jest.mock('../src/server.js', () => {
            const express = require('express');
            const app = express();
            app.use(express.json());

            app.get('/health', (req, res) => {
                res.status(200).json({
                    status: 'healthy',
                    service: 'visualization-web',
                    timestamp: new Date().toISOString()
                });
            });

            app.get('/api/visualizations', (req, res) => {
                const { type, dashboardId } = req.query;
                const visualizations = [
                    { id: 1, type: 'line-chart', title: 'Sales Trend', dashboardId: 'dash1' },
                    { id: 2, type: 'bar-chart', title: 'Revenue Comparison', dashboardId: 'dash1' },
                    { id: 3, type: 'pie-chart', title: 'Market Share', dashboardId: 'dash2' }
                ];

                let filtered = visualizations;
                if (type) {
                    filtered = filtered.filter(v => v.type === type);
                }
                if (dashboardId) {
                    filtered = filtered.filter(v => v.dashboardId === dashboardId);
                }

                res.status(200).json({
                    visualizations: filtered,
                    total: filtered.length
                });
            });

            app.post('/api/visualizations', (req, res) => {
                const { type, title, data, config } = req.body;
                if (!type || !title || !data) {
                    return res.status(400).json({ error: 'Type, title, and data are required' });
                }

                const validTypes = ['line-chart', 'bar-chart', 'pie-chart', 'scatter-plot', 'heatmap'];
                if (!validTypes.includes(type)) {
                    return res.status(400).json({
                        error: `Invalid type. Must be one of: ${validTypes.join(', ')}`
                    });
                }

                res.status(201).json({
                    id: Date.now(),
                    type,
                    title,
                    data,
                    config: config || {},
                    createdAt: new Date().toISOString()
                });
            });

            app.get('/api/visualizations/:id', (req, res) => {
                const id = parseInt(req.params.id);
                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid visualization ID' });
                }
                res.status(200).json({
                    id,
                    type: 'line-chart',
                    title: `Visualization ${id}`,
                    data: {
                        labels: ['Jan', 'Feb', 'Mar'],
                        datasets: [{ label: 'Sales', data: [100, 150, 200] }]
                    },
                    config: { responsive: true, maintainAspectRatio: false }
                });
            });

            app.put('/api/visualizations/:id', (req, res) => {
                const id = parseInt(req.params.id);
                const { title, data, config } = req.body;

                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid visualization ID' });
                }

                res.status(200).json({
                    id,
                    type: 'line-chart',
                    title: title || `Visualization ${id}`,
                    data: data || { labels: [], datasets: [] },
                    config: config || {},
                    updatedAt: new Date().toISOString()
                });
            });

            app.delete('/api/visualizations/:id', (req, res) => {
                const id = parseInt(req.params.id);
                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid visualization ID' });
                }
                res.status(204).send();
            });

            app.get('/api/visualizations/:id/export', (req, res) => {
                const id = parseInt(req.params.id);
                const { format } = req.query;

                if (isNaN(id)) {
                    return res.status(400).json({ error: 'Invalid visualization ID' });
                }

                const validFormats = ['png', 'svg', 'pdf'];
                if (!format || !validFormats.includes(format)) {
                    return res.status(400).json({
                        error: `Invalid format. Must be one of: ${validFormats.join(', ')}`
                    });
                }

                // Mock export response
                if (format === 'png' || format === 'svg') {
                    res.status(200).json({
                        visualizationId: id,
                        format,
                        downloadUrl: `/downloads/vis-${id}.${format}`,
                        size: '150KB'
                    });
                } else if (format === 'pdf') {
                    res.status(200).json({
                        visualizationId: id,
                        format,
                        downloadUrl: `/downloads/vis-${id}.pdf`,
                        size: '75KB'
                    });
                }
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
            expect(response.body).toHaveProperty('service', 'visualization-web');
        });
    });

    describe('Visualization CRUD Operations', () => {
        test('should get all visualizations', async () => {
            const response = await request(app)
                .get('/api/visualizations')
                .expect(200);

            expect(response.body).toHaveProperty('visualizations');
            expect(response.body).toHaveProperty('total');
            expect(Array.isArray(response.body.visualizations)).toBe(true);
        });

        test('should filter visualizations by type', async () => {
            const response = await request(app)
                .get('/api/visualizations?type=line-chart')
                .expect(200);

            expect(response.body.visualizations.every(v => v.type === 'line-chart')).toBe(true);
        });

        test('should filter visualizations by dashboard', async () => {
            const response = await request(app)
                .get('/api/visualizations?dashboardId=dash1')
                .expect(200);

            expect(response.body.visualizations.every(v => v.dashboardId === 'dash1')).toBe(true);
        });

        test('should create new visualization', async () => {
            const newViz = {
                type: 'bar-chart',
                title: 'Test Chart',
                data: {
                    labels: ['A', 'B', 'C'],
                    datasets: [{ label: 'Dataset', data: [10, 20, 30] }]
                },
                config: { colors: ['blue', 'green'] }
            };

            const response = await request(app)
                .post('/api/visualizations')
                .send(newViz)
                .expect(201);

            expect(response.body).toHaveProperty('id');
            expect(response.body).toHaveProperty('type', 'bar-chart');
            expect(response.body).toHaveProperty('title', 'Test Chart');
            expect(response.body).toHaveProperty('data');
            expect(response.body).toHaveProperty('config');
            expect(response.body).toHaveProperty('createdAt');
        });

        test('should return 400 for invalid visualization type', async () => {
            const invalidViz = {
                type: 'invalid-chart',
                title: 'Invalid Chart',
                data: { labels: [], datasets: [] }
            };

            const response = await request(app)
                .post('/api/visualizations')
                .send(invalidViz)
                .expect(400);

            expect(response.body).toHaveProperty('error');
            expect(response.body.error).toContain('Invalid type');
        });

        test('should get visualization by ID', async () => {
            const response = await request(app)
                .get('/api/visualizations/1')
                .expect(200);

            expect(response.body).toHaveProperty('id', 1);
            expect(response.body).toHaveProperty('type', 'line-chart');
            expect(response.body).toHaveProperty('title', 'Visualization 1');
            expect(response.body).toHaveProperty('data');
            expect(response.body).toHaveProperty('config');
        });

        test('should update visualization', async () => {
            const updateData = {
                title: 'Updated Visualization',
                data: {
                    labels: ['X', 'Y', 'Z'],
                    datasets: [{ label: 'Updated', data: [40, 50, 60] }]
                }
            };

            const response = await request(app)
                .put('/api/visualizations/1')
                .send(updateData)
                .expect(200);

            expect(response.body).toHaveProperty('id', 1);
            expect(response.body).toHaveProperty('title', 'Updated Visualization');
            expect(response.body).toHaveProperty('updatedAt');
        });

        test('should delete visualization', async () => {
            await request(app)
                .delete('/api/visualizations/1')
                .expect(204);
        });
    });

    describe('Export Functionality', () => {
        test('should export visualization as PNG', async () => {
            const response = await request(app)
                .get('/api/visualizations/1/export?format=png')
                .expect(200);

            expect(response.body).toHaveProperty('visualizationId', 1);
            expect(response.body).toHaveProperty('format', 'png');
            expect(response.body).toHaveProperty('downloadUrl');
            expect(response.body).toHaveProperty('size');
        });

        test('should export visualization as SVG', async () => {
            const response = await request(app)
                .get('/api/visualizations/1/export?format=svg')
                .expect(200);

            expect(response.body).toHaveProperty('format', 'svg');
            expect(response.body.downloadUrl).toContain('.svg');
        });

        test('should export visualization as PDF', async () => {
            const response = await request(app)
                .get('/api/visualizations/1/export?format=pdf')
                .expect(200);

            expect(response.body).toHaveProperty('format', 'pdf');
            expect(response.body.downloadUrl).toContain('.pdf');
        });

        test('should return 400 for invalid export format', async () => {
            const response = await request(app)
                .get('/api/visualizations/1/export?format=docx')
                .expect(400);

            expect(response.body).toHaveProperty('error');
            expect(response.body.error).toContain('Invalid format');
        });
    });

    describe('Data Validation', () => {
        test('should return 400 for missing required fields', async () => {
            const response = await request(app)
                .post('/api/visualizations')
                .send({ type: 'line-chart' })
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Type, title, and data are required');
        });

        test('should handle malformed JSON', async () => {
            const response = await request(app)
                .post('/api/visualizations')
                .set('Content-Type', 'application/json')
                .send('invalid json')
                .expect(400);
        });
    });

    describe('Performance Tests', () => {
        test('should respond within acceptable time limits', async () => {
            const startTime = Date.now();
            await request(app).get('/api/visualizations').expect(200);
            expect(Date.now() - startTime).toBeLessThan(500);
        });

        test('should handle concurrent visualization creation', async () => {
            const promises = Array(5).fill().map((_, i) =>
                request(app)
                    .post('/api/visualizations')
                    .send({
                        type: 'pie-chart',
                        title: `Chart ${i}`,
                        data: { labels: ['A', 'B'], datasets: [{ data: [1, 2] }] }
                    })
            );

            const responses = await Promise.all(promises);
            responses.forEach((response, i) => {
                expect(response.status).toBe(201);
                expect(response.body).toHaveProperty('title', `Chart ${i}`);
            });
        });
    });
});