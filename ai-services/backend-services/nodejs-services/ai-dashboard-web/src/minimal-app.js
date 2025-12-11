const express = require('express');
const cors = require('cors');

const app = express();

// Basic middleware
app.use(cors());
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: new Date().toISOString(),
        service: 'ai-dashboard-web',
        version: '1.0.0',
        port: 3000,
        uptime: process.uptime()
    });
});

// Analytics endpoints
app.get('/api/v1/analytics/events', (req, res) => {
    res.json({
        success: true,
        data: {
            events: [
                {
                    id: '1',
                    type: 'model_prediction',
                    service: 'ai-dashboard-web',
                    timestamp: new Date().toISOString(),
                    properties: { confidence: 0.95 }
                },
                {
                    id: '2',
                    type: 'user_interaction',
                    service: 'ai-dashboard-web',
                    timestamp: new Date().toISOString(),
                    properties: { action: 'viewed_dashboard' }
                }
            ],
            count: 2
        }
    });
});

// Dashboard endpoints
app.get('/api/v1/dashboard/overview', (req, res) => {
    res.json({
        success: true,
        data: {
            totalModels: 5,
            activeTrainings: 2,
            predictionsToday: 123,
            systemHealth: 'healthy',
            services: [
                { name: 'ai-dashboard-web', status: 'active', port: 3000 },
                { name: 'ai-training-service', status: 'active', port: 3001 },
                { name: 'ai-gateway', status: 'active', port: 3002 }
            ]
        }
    });
});

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        message: 'Welcome to AI Dashboard API',
        version: '1.0.0',
        service: 'ai-dashboard-web',
        endpoints: {
            health: '/health',
            analytics: '/api/v1/analytics',
            dashboard: '/api/v1/dashboard'
        }
    });
});

// Error handling
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        success: false,
        error: 'Internal Server Error',
        timestamp: new Date().toISOString()
    });
});

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`\n🚀 AI Dashboard running on http://localhost:${PORT}`);
    console.log(`📊 Analytics: http://localhost:${PORT}/api/v1/analytics/events`);
    console.log(`📈 Dashboard: http://localhost:${PORT}/api/v1/dashboard/overview`);
    console.log(`💚 Health Check: http://localhost:${PORT}/health`);
    console.log(`⏰ Started at: ${new Date()}\n`);
});

module.exports = app;