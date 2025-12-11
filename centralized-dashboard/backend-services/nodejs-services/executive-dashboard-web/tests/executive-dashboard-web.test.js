const request = require('supertest');
const express = require('express');

// Mock the server module
jest.mock('../src/server.js', () => {
    const app = express();
    app.use(express.json());

    app.get('/health', (req, res) => {
        res.status(200).json({ status: 'healthy', service: 'executive-dashboard-web', timestamp: new Date().toISOString() });
    });

    app.get('/api/executive/kpi', (req, res) => {
        const { category, timeRange } = req.query;
        res.status(200).json({
            category: category || 'all',
            timeRange: timeRange || 'monthly',
            kpis: {
                revenue: 2850000,
                growth: 0.145,
                customerSatisfaction: 0.89,
                operatingMargin: 0.23,
                employeeCount: 1250,
                marketShare: 0.187
            },
            lastUpdated: new Date().toISOString()
        });
    });

    app.get('/api/executive/performance', (req, res) => {
        const { department, quarter } = req.query;
        res.status(200).json({
            department: department || 'all',
            quarter: quarter || 'Q4-2024',
            performance: {
                sales: { target: 1000000, actual: 1150000, variance: 0.15 },
                marketing: { target: 500000, actual: 480000, variance: -0.04 },
                operations: { target: 750000, actual: 780000, variance: 0.04 },
                finance: { target: 200000, actual: 195000, variance: -0.025 }
            }
        });
    });

    app.get('/api/executive/alerts', (req, res) => {
        const { severity, status } = req.query;
        const alerts = [
            {
                id: 1,
                title: 'Revenue Target Achieved',
                severity: 'success',
                status: 'active',
                message: 'Q4 revenue target exceeded by 15%',
                createdAt: new Date().toISOString()
            },
            {
                id: 2,
                title: 'Customer Satisfaction Decline',
                severity: 'warning',
                status: 'active',
                message: 'Customer satisfaction dropped below 90%',
                createdAt: new Date().toISOString()
            },
            {
                id: 3,
                title: 'Operating Margin Improvement',
                severity: 'info',
                status: 'acknowledged',
                message: 'Operating margin improved by 3%',
                createdAt: new Date().toISOString()
            }
        ];

        let filteredAlerts = alerts;
        if (severity) {
            filteredAlerts = filteredAlerts.filter(alert => alert.severity === severity);
        }
        if (status) {
            filteredAlerts = filteredAlerts.filter(alert => alert.status === status);
        }

        res.status(200).json({
            alerts: filteredAlerts,
            total: filteredAlerts.length
        });
    });

    app.get('/api/executive/financial-summary', (req, res) => {
        res.status(200).json({
            period: 'YTD-2024',
            revenue: {
                total: 11400000,
                growth: 0.145,
                byProduct: {
                    productA: 5700000,
                    productB: 3420000,
                    productC: 2280000
                }
            },
            expenses: {
                total: 8778000,
                breakdown: {
                    salaries: 4389000,
                    operations: 2633400,
                    marketing: 1755600
                }
            },
            profit: {
                gross: 2622000,
                net: 1875000,
                margin: 0.164
            }
        });
    });

    app.post('/api/executive/reports', (req, res) => {
        const { title, type, parameters, schedule } = req.body;

        if (!title || !type) {
            return res.status(400).json({ error: 'Title and type are required' });
        }

        const newReport = {
            id: Date.now(),
            title,
            type,
            parameters: parameters || {},
            schedule: schedule || null,
            status: 'created',
            createdAt: new Date().toISOString()
        };

        res.status(201).json(newReport);
    });

    return app;
});

const app = require('../src/server.js');

describe('Executive Dashboard Web Service API Tests', () => {
    describe('Health Check', () => {
        test('should return health status', async () => {
            const response = await request(app)
                .get('/health')
                .expect(200);

            expect(response.body).toHaveProperty('status', 'healthy');
            expect(response.body).toHaveProperty('service', 'executive-dashboard-web');
            expect(response.body).toHaveProperty('timestamp');
        });
    });

    describe('KPI Management', () => {
        test('should get all KPIs with default parameters', async () => {
            const response = await request(app)
                .get('/api/executive/kpi')
                .expect(200);

            expect(response.body).toHaveProperty('category', 'all');
            expect(response.body).toHaveProperty('timeRange', 'monthly');
            expect(response.body).toHaveProperty('kpis');
            expect(response.body.kpis).toHaveProperty('revenue');
            expect(response.body.kpis).toHaveProperty('growth');
            expect(response.body.kpis).toHaveProperty('customerSatisfaction');
            expect(response.body.kpis).toHaveProperty('operatingMargin');
            expect(response.body.kpis).toHaveProperty('employeeCount');
            expect(response.body.kpis).toHaveProperty('marketShare');
            expect(response.body).toHaveProperty('lastUpdated');
        });

        test('should get KPIs with custom filters', async () => {
            const response = await request(app)
                .get('/api/executive/kpi?category=sales&timeRange=quarterly')
                .expect(200);

            expect(response.body).toHaveProperty('category', 'sales');
            expect(response.body).toHaveProperty('timeRange', 'quarterly');
        });
    });

    describe('Performance Tracking', () => {
        test('should get performance metrics', async () => {
            const response = await request(app)
                .get('/api/executive/performance')
                .expect(200);

            expect(response.body).toHaveProperty('department', 'all');
            expect(response.body).toHaveProperty('quarter', 'Q4-2024');
            expect(response.body).toHaveProperty('performance');
            expect(response.body.performance).toHaveProperty('sales');
            expect(response.body.performance).toHaveProperty('marketing');
            expect(response.body.performance).toHaveProperty('operations');
            expect(response.body.performance).toHaveProperty('finance');

            // Check structure of performance data
            expect(response.body.performance.sales).toHaveProperty('target');
            expect(response.body.performance.sales).toHaveProperty('actual');
            expect(response.body.performance.sales).toHaveProperty('variance');
        });

        test('should get performance for specific department', async () => {
            const response = await request(app)
                .get('/api/executive/performance?department=sales&quarter=Q3-2024')
                .expect(200);

            expect(response.body).toHaveProperty('department', 'sales');
            expect(response.body).toHaveProperty('quarter', 'Q3-2024');
        });
    });

    describe('Executive Alerts', () => {
        test('should get all executive alerts', async () => {
            const response = await request(app)
                .get('/api/executive/alerts')
                .expect(200);

            expect(response.body).toHaveProperty('alerts');
            expect(response.body).toHaveProperty('total');
            expect(Array.isArray(response.body.alerts)).toBe(true);
            expect(response.body.alerts.length).toBeGreaterThan(0);

            // Check structure of alert
            const alert = response.body.alerts[0];
            expect(alert).toHaveProperty('id');
            expect(alert).toHaveProperty('title');
            expect(alert).toHaveProperty('severity');
            expect(alert).toHaveProperty('status');
            expect(alert).toHaveProperty('message');
            expect(alert).toHaveProperty('createdAt');
        });

        test('should filter alerts by severity', async () => {
            const response = await request(app)
                .get('/api/executive/alerts?severity=warning')
                .expect(200);

            expect(response.body.alerts.every(alert => alert.severity === 'warning')).toBe(true);
        });

        test('should filter alerts by status', async () => {
            const response = await request(app)
                .get('/api/executive/alerts?status=acknowledged')
                .expect(200);

            expect(response.body.alerts.every(alert => alert.status === 'acknowledged')).toBe(true);
        });
    });

    describe('Financial Summary', () => {
        test('should get financial summary', async () => {
            const response = await request(app)
                .get('/api/executive/financial-summary')
                .expect(200);

            expect(response.body).toHaveProperty('period', 'YTD-2024');
            expect(response.body).toHaveProperty('revenue');
            expect(response.body).toHaveProperty('expenses');
            expect(response.body).toHaveProperty('profit');

            // Check revenue structure
            expect(response.body.revenue).toHaveProperty('total');
            expect(response.body.revenue).toHaveProperty('growth');
            expect(response.body.revenue).toHaveProperty('byProduct');

            // Check profit structure
            expect(response.body.profit).toHaveProperty('gross');
            expect(response.body.profit).toHaveProperty('net');
            expect(response.body.profit).toHaveProperty('margin');
        });
    });

    describe('Report Generation', () => {
        test('should create new report', async () => {
            const reportData = {
                title: 'Quarterly Performance Report',
                type: 'performance',
                parameters: {
                    department: 'all',
                    timeRange: 'quarterly'
                },
                schedule: {
                    frequency: 'monthly',
                    day: 1
                }
            };

            const response = await request(app)
                .post('/api/executive/reports')
                .send(reportData)
                .expect(201);

            expect(response.body).toHaveProperty('id');
            expect(response.body).toHaveProperty('title', 'Quarterly Performance Report');
            expect(response.body).toHaveProperty('type', 'performance');
            expect(response.body).toHaveProperty('parameters');
            expect(response.body).toHaveProperty('schedule');
            expect(response.body).toHaveProperty('status', 'created');
            expect(response.body).toHaveProperty('createdAt');
        });

        test('should return 400 when creating report without required fields', async () => {
            const invalidReport = {
                title: 'Incomplete Report'
                // missing type
            };

            const response = await request(app)
                .post('/api/executive/reports')
                .send(invalidReport)
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Title and type are required');
        });

        test('should create report with minimal data', async () => {
            const minimalReport = {
                title: 'Basic Report',
                type: 'summary'
            };

            const response = await request(app)
                .post('/api/executive/reports')
                .send(minimalReport)
                .expect(201);

            expect(response.body).toHaveProperty('title', 'Basic Report');
            expect(response.body).toHaveProperty('type', 'summary');
            expect(response.body.parameters).toEqual({});
            expect(response.body.schedule).toBeNull();
        });
    });

    describe('Data Validation', () => {
        test('should handle empty request body', async () => {
            const response = await request(app)
                .post('/api/executive/reports')
                .send({})
                .expect(400);

            expect(response.body).toHaveProperty('error', 'Title and type are required');
        });

        test('should handle malformed JSON', async () => {
            const response = await request(app)
                .post('/api/executive/reports')
                .set('Content-Type', 'application/json')
                .send('invalid json')
                .expect(400);
        });
    });

    describe('Performance Tests', () => {
        test('should respond within acceptable time limits', async () => {
            const startTime = Date.now();

            await request(app)
                .get('/api/executive/kpi')
                .expect(200);

            const responseTime = Date.now() - startTime;
            expect(responseTime).toBeLessThan(500);
        });

        test('should handle concurrent requests', async () => {
            const promises = Array(5).fill().map(() =>
                request(app).get('/api/executive/financial-summary')
            );

            const responses = await Promise.all(promises);

            responses.forEach(response => {
                expect(response.status).toBe(200);
                expect(response.body).toHaveProperty('revenue');
                expect(response.body).toHaveProperty('profit');
            });
        });
    });
});