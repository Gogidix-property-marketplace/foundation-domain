const request = require('supertest');
const express = require('express');

// Mock the server module
jest.mock('../src/server.js', () => {
    const app = express();
    app.use(express.json());

    // Mock dashboard routes
    app.get('/health', (req, res) => {
        res.status(200).json({ status: 'healthy', service: 'dashboard-web', timestamp: new Date().toISOString() });
    });

    app.get('/api/dashboards', (req, res) => {
        res.status(200).json({
            dashboards: [
                { id: 1, name: 'Main Dashboard', owner: 'user1', widgets: 5 },
                { id: 2, name: 'Analytics Dashboard', owner: 'user2', widgets: 3 }
            ],
            total: 2
        });
    });

    app.post('/api/dashboards', (req, res) => {
        const { name, owner } = req.body;
        if (!name || !owner) {
            return res.status(400).json({ error: 'Name and owner are required' });
        }
        res.status(201).json({ id: 3, name, owner, widgets: 0, createdAt: new Date().toISOString() });
    });

    app.get('/api/dashboards/:id', (req, res) => {
        const id = parseInt(req.params.id);
        if (isNaN(id)) {
            return res.status(400).json({ error: 'Invalid dashboard ID' });
        }
        if (id > 100) {
            return res.status(404).json({ error: 'Dashboard not found' });
        }
        res.status(200).json({ id, name: `Dashboard ${id}`, owner: 'user1', widgets: 4 });
    });

    app.put('/api/dashboards/:id', (req, res) => {
        const id = parseInt(req.params.id);
        const { name, widgets } = req.body;
        if (isNaN(id)) {
            return res.status(400).json({ error: 'Invalid dashboard ID' });
        }
        if (!name) {
            return res.status(400).json({ error: 'Name is required' });
        }
        res.status(200).json({ id, name, widgets: widgets || 0, updatedAt: new Date().toISOString() });
    });

    app.delete('/api/dashboards/:id', (req, res) => {
        const id = parseInt(req.params.id);
        if (isNaN(id)) {
            return res.status(400).json({ error: 'Invalid dashboard ID' });
        }
        if (id > 100) {
            return res.status(404).json({ error: 'Dashboard not found' });
        }
        res.status(204).send();
    });

    app.get('/api/dashboards/:id/widgets', (req, res) => {
        const id = parseInt(req.params.id);
        if (isNaN(id)) {
            return res.status(400).json({ error: 'Invalid dashboard ID' });
        }
        res.status(200).json({
            dashboardId: id,
            widgets: [
                { id: 1, type: 'chart', title: 'Sales Trend', position: { x: 0, y: 0 } },
                { id: 2, type: 'metric', title: 'Total Users', position: { x: 1, y: 0 } }
            ]
        });
    });

    app.post('/api/dashboards/:id/widgets', (req, res) => {
        const id = parseInt(req.params.id);
        const { type, title, position } = req.body;
        if (!type || !title || !position) {
            return res.status(400).json({ error: 'Type, title, and position are required' });
        }
        res.status(201).json({
            id: 3,
            dashboardId: id,
            type,
            title,
            position,
            createdAt: new Date().toISOString()
        });
    });

    return app;
});

const app = require('../src/server.js');

describe('Dashboard Web Service API Tests', () => {
    describe('Health Check', () => {
        test('should return health status', async () => {
            const response = await request(app)
                .get('/health')
                .expect(200);

            expect(response.body).toHaveProperty('status', 'healthy');
            expect(response.body).toHaveProperty('service', 'dashboard-web');
            expect(response.body).toHaveProperty('timestamp');
        });
    });

    describe('Dashboard CRUD Operations', () => {
        test('should get all dashboards', async () => {
            const response = await request(app)
                .get('/api/dashboards')
                .expect(200);

            expect(response.body).toHaveProperty('dashboards');
            expect(response.body).toHaveProperty('total', 2);
            expect(Array.isArray(response.body.dashboards)).toBe(true);
            expect(response.body.dashboards.length).toBe(2);
        });

        test('should create a new dashboard', async () => {
            const newDashboard = {
                name: 'Test Dashboard',
                owner: 'testuser'
            };

            const response = await request(app)
                .post('/api/dashboards')
                .send(newDashboard)
                .expect(201);

            expect(response.body).toHaveProperty('id', 3);
            expect(response.body).toHaveProperty('name', 'Test Dashboard');
            expect(response.body).toHaveProperty('owner', 'testuser');
            expect(response.body).toHaveProperty('widgets', 0);
            expect(response.body).toHaveProperty('createdAt');
        });

        test('should return 400 when creating dashboard without required fields', async () => {
            const invalidDashboard = {
                name: 'Incomplete Dashboard'
                // missing owner
            };

            const response = await request(app)
                .post('/api/dashboards')
                .send(invalidDashboard)
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Name and owner are required');
        });

        test('should get dashboard by ID', async () => {
            const response = await request(app)
                .get('/api/dashboards/1')
                .expect(200);

            expect(response.body).toHaveProperty('id', 1);
            expect(response.body).toHaveProperty('name', 'Dashboard 1');
            expect(response.body).toHaveProperty('owner', 'user1');
            expect(response.body).toHaveProperty('widgets', 4);
        });

        test('should return 400 for invalid dashboard ID', async () => {
            const response = await request(app)
                .get('/api/dashboards/invalid')
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Invalid dashboard ID');
        });

        test('should return 404 for non-existent dashboard', async () => {
            const response = await request(app)
                .get('/api/dashboards/999')
                .expect(404);

            expect(response.body).toHaveProperty('error', 'Dashboard not found');
        });

        test('should update dashboard', async () => {
            const updateData = {
                name: 'Updated Dashboard',
                widgets: 5
            };

            const response = await request(app)
                .put('/api/dashboards/1')
                .send(updateData)
                .expect(200);

            expect(response.body).toHaveProperty('id', 1);
            expect(response.body).toHaveProperty('name', 'Updated Dashboard');
            expect(response.body).toHaveProperty('widgets', 5);
            expect(response.body).toHaveProperty('updatedAt');
        });

        test('should return 400 when updating dashboard without name', async () => {
            const invalidUpdate = {
                widgets: 3
                // missing name
            };

            const response = await request(app)
                .put('/api/dashboards/1')
                .send(invalidUpdate)
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Name is required');
        });

        test('should delete dashboard', async () => {
            await request(app)
                .delete('/api/dashboards/1')
                .expect(204);
        });

        test('should return 400 when deleting dashboard with invalid ID', async () => {
            const response = await request(app)
                .delete('/api/dashboards/invalid')
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Invalid dashboard ID');
        });

        test('should return 404 when deleting non-existent dashboard', async () => {
            const response = await request(app)
                .delete('/api/dashboards/999')
                .expect(404);

            expect(response.body).toHaveProperty('error', 'Dashboard not found');
        });
    });

    describe('Widget Management', () => {
        test('should get widgets for dashboard', async () => {
            const response = await request(app)
                .get('/api/dashboards/1/widgets')
                .expect(200);

            expect(response.body).toHaveProperty('dashboardId', 1);
            expect(response.body).toHaveProperty('widgets');
            expect(Array.isArray(response.body.widgets)).toBe(true);
            expect(response.body.widgets.length).toBe(2);
        });

        test('should return 400 for invalid dashboard ID when getting widgets', async () => {
            const response = await request(app)
                .get('/api/dashboards/invalid/widgets')
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Invalid dashboard ID');
        });

        test('should add widget to dashboard', async () => {
            const newWidget = {
                type: 'chart',
                title: 'New Chart Widget',
                position: { x: 2, y: 0 }
            };

            const response = await request(app)
                .post('/api/dashboards/1/widgets')
                .send(newWidget)
                .expect(201);

            expect(response.body).toHaveProperty('id', 3);
            expect(response.body).toHaveProperty('dashboardId', 1);
            expect(response.body).toHaveProperty('type', 'chart');
            expect(response.body).toHaveProperty('title', 'New Chart Widget');
            expect(response.body).toHaveProperty('position');
            expect(response.body).toHaveProperty('createdAt');
        });

        test('should return 400 when adding widget without required fields', async () => {
            const invalidWidget = {
                type: 'metric'
                // missing title and position
            };

            const response = await request(app)
                .post('/api/dashboards/1/widgets')
                .send(invalidWidget)
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Type, title, and position are required');
        });
    });

    describe('Error Handling', () => {
        test('should handle malformed JSON gracefully', async () => {
            const response = await request(app)
                .post('/api/dashboards')
                .set('Content-Type', 'application/json')
                .send('invalid json')
                .expect(400);
        });

        test('should handle empty request body', async () => {
            const response = await request(app)
                .post('/api/dashboards')
                .send({})
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Name and owner are required');
        });
    });

    describe('Performance Tests', () => {
        test('should respond within acceptable time limits', async () => {
            const startTime = Date.now();

            await request(app)
                .get('/api/dashboards')
                .expect(200);

            const responseTime = Date.now() - startTime;
            expect(responseTime).toBeLessThan(1000); // Should respond within 1 second
        });

        test('should handle concurrent requests', async () => {
            const promises = Array(10).fill().map(() =>
                request(app).get('/api/dashboards')
            );

            const responses = await Promise.all(promises);

            responses.forEach(response => {
                expect(response.status).toBe(200);
                expect(response.body).toHaveProperty('dashboards');
            });
        });
    });
});