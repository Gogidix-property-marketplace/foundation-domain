/**
 * Management Domain Integration Service
 * Port 3003
 *
 * Connects AI services to management domains:
 * - Property Management Integration
 * - User Management Integration
 * - Financial Services Integration
 * - Real-time data synchronization
 * - Event-driven architecture
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const { body, query, validationResult } = require('express-validator');
const axios = require('axios');
const winston = require('winston');
const path = require('path');
const NodeCache = require('node-cache');
const { v4: uuidv4 } = require('uuid');
const _ = require('lodash');
const Joi = require('joi');
const moment = require('moment');

// Configuration
const config = {
    port: process.env.MGMT_PORT || 3003,
    env: process.env.NODE_ENV || 'development',
    logLevel: process.env.LOG_LEVEL || 'info',

    // AI Gateway Connection
    aiGateway: {
        baseUrl: 'http://localhost:3002',
        timeout: 30000
    },

    // Management Domain Endpoints
    domains: {
        propertyManagement: {
            baseUrl: process.env.PROPERTY_MGMT_URL || 'http://localhost:3010',
            endpoints: {
                properties: '/api/v1/properties',
                listings: '/api/v1/listings',
                analytics: '/api/v1/analytics'
            }
        },
        userManagement: {
            baseUrl: process.env.USER_MGMT_URL || 'http://localhost:3011',
            endpoints: {
                users: '/api/v1/users',
                profiles: '/api/v1/profiles',
                preferences: '/api/v1/preferences',
                activity: '/api/v1/activity'
            }
        },
        financialServices: {
            baseUrl: process.env.FINANCIAL_URL || 'http://localhost:3012',
            endpoints: {
                transactions: '/api/v1/transactions',
                analytics: '/api/v1/analytics',
                reports: '/api/v1/reports',
                forecasts: '/api/v1/forecasts'
            }
        }
    },

    // Integration Settings
    integration: {
        syncInterval: 300000, // 5 minutes
        batchSize: 100,
        retryAttempts: 3,
        retryDelay: 5000
    },

    // Cache Configuration
    cache: {
        stdTTL: 600, // 10 minutes
        checkperiod: 60,
        useClones: false
    }
};

// Initialize Express App
const app = express();

// Configure Winston Logger
const logger = winston.createLogger({
    level: config.logLevel,
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.errors({ stack: true }),
        winston.format.json()
    ),
    defaultMeta: { service: 'management-domain-integration' },
    transports: [
        new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
        new winston.transports.File({ filename: 'logs/combined.log' }),
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.simple()
            )
        })
    ]
});

// Initialize Cache
const cache = new NodeCache(config.cache);

// AI Service Client
class AIGatewayClient {
    constructor() {
        this.baseUrl = config.aiGateway.baseUrl;
        this.timeout = config.aiGateway.timeout;
    }

    async callService(service, endpoint, data = {}, method = 'POST') {
        try {
            const response = await axios({
                method,
                url: `${this.baseUrl}/api/v1/ai/unified`,
                timeout: this.timeout,
                data: {
                    service,
                    endpoint,
                    data,
                    method,
                    options: { cache: true }
                },
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            return response.data;
        } catch (error) {
            logger.error('AI Gateway call failed', {
                service,
                endpoint,
                error: error.message
            });
            throw error;
        }
    }

    async batchCall(requests) {
        try {
            const response = await axios({
                method: 'POST',
                url: `${this.baseUrl}/api/v1/ai/batch`,
                timeout: this.timeout * 2,
                data: { requests },
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            return response.data;
        } catch (error) {
            logger.error('AI Gateway batch call failed', {
                requests: requests.length,
                error: error.message
            });
            throw error;
        }
    }
}

// Domain Client Classes
class PropertyManagementClient {
    constructor(aiClient) {
        this.aiClient = aiClient;
        this.baseUrl = config.domains.propertyManagement.baseUrl;
    }

    async enrichPropertyWithAI(propertyId) {
        try {
            // Get property data
            const propertyResponse = await axios.get(`${this.baseUrl}${config.domains.propertyManagement.endpoints.properties}/${propertyId}`);
            const property = propertyResponse.data;

            // Batch AI processing
            const aiRequests = [];

            // Image analysis if images exist
            if (property.images && property.images.length > 0) {
                aiRequests.push({
                    service: 'image-analysis',
                    endpoint: '/api/v1/analyze',
                    data: { images: property.images, analysis_type: 'comprehensive' }
                });
            }

            // Text analysis for description
            if (property.description) {
                aiRequests.push({
                    service: 'text-analysis',
                    endpoint: '/api/v1/analyze',
                    data: { text: property.description, analysis_type: 'comprehensive' }
                });
            }

            // Price prediction
            aiRequests.push({
                service: 'predictive-analytics',
                endpoint: '/api/v1/predict',
                data: {
                    metric: 'property_value',
                    historical_data: property.priceHistory,
                    features: {
                        location: property.location,
                        bedrooms: property.bedrooms,
                        bathrooms: property.bathrooms,
                        area: property.area,
                        type: property.type
                    }
                }
            });

            // Execute AI requests
            const aiResults = await this.aiClient.batchCall(aiRequests);

            // Process and merge results
            const enrichedProperty = {
                ...property,
                aiAnalysis: {
                    timestamp: new Date().toISOString(),
                    insights: {}
                }
            };

            // Process image analysis
            const imageResult = aiResults.results.find(r => r.data.data.room_detection);
            if (imageResult) {
                enrichedProperty.aiAnalysis.insights.images = imageResult.data.data;
            }

            // Process text analysis
            const textResult = aiResults.results.find(r => r.data.data.sentiment);
            if (textResult) {
                enrichedProperty.aiAnalysis.insights.description = textResult.data.data;
            }

            // Process price prediction
            const predictionResult = aiResults.results.find(r => r.data.data.forecast);
            if (predictionResult) {
                enrichedProperty.aiAnalysis.insights.pricing = predictionResult.data.data;
            }

            // Update property with AI insights
            await axios.put(
                `${this.baseUrl}${config.domains.propertyManagement.endpoints.properties}/${propertyId}`,
                enrichedProperty
            );

            return enrichedProperty;

        } catch (error) {
            logger.error('Property enrichment failed', {
                propertyId,
                error: error.message
            });
            throw error;
        }
    }

    async generatePropertyRecommendations(userId) {
        try {
            // Get user's property preferences
            const userResponse = await axios.get(`${config.domains.userManagement.baseUrl}${config.domains.userManagement.endpoints.preferences}/${userId}`);
            const preferences = userResponse.data;

            // Get recommendation from AI service
            const recommendations = await this.aiClient.callService(
                'recommendation',
                '/api/v1/recommendations/properties',
                {
                    user_id: userId,
                    preferences,
                    limit: 10
                }
            );

            return recommendations;

        } catch (error) {
            logger.error('Property recommendations failed', {
                userId,
                error: error.message
            });
            throw error;
        }
    }
}

class UserManagementClient {
    constructor(aiClient) {
        this.aiClient = aiClient;
        this.baseUrl = config.domains.userManagement.baseUrl;
    }

    async analyzeUserBehavior(userId) {
        try {
            // Get user activity data
            const activityResponse = await axios.get(`${this.baseUrl}${config.domains.userManagement.endpoints.activity}/${userId}`);
            const activities = activityResponse.data;

            // Get user profile
            const profileResponse = await axios.get(`${this.baseUrl}${config.domains.userManagement.endpoints.profiles}/${userId}`);
            const profile = profileResponse.data;

            // Analyze behavior with AI
            const behaviorAnalysis = await this.aiClient.callService(
                'predictive-analytics',
                '/api/v1/predict',
                {
                    metric: 'user_engagement',
                    historical_data: activities,
                    features: {
                        profile,
                        demographics: profile.demographics,
                        preferences: profile.preferences
                    }
                }
            );

            // Update user profile with insights
            await axios.patch(
                `${this.baseUrl}${config.domains.userManagement.endpoints.profiles}/${userId}`,
                {
                    aiInsights: behaviorAnalysis.data,
                    lastAnalysisDate: new Date().toISOString()
                }
            );

            return behaviorAnalysis;

        } catch (error) {
            logger.error('User behavior analysis failed', {
                userId,
                error: error.message
            });
            throw error;
        }
    }

    async detectFraudulentActivity(userId, activity) {
        try {
            const fraudAnalysis = await this.aiClient.callService(
                'fraud-detection',
                '/api/v1/analyze',
                {
                    user_id: userId,
                    activity,
                    context: {
                        device: activity.device,
                        location: activity.location,
                        timestamp: activity.timestamp
                    }
                }
            );

            // If fraud detected, create alert
            if (fraudAnalysis.data.risk_score > 70) {
                await axios.post(`${this.baseUrl}/api/v1/security/alerts`, {
                    userId,
                    riskScore: fraudAnalysis.data.risk_score,
                    reasons: fraudAnalysis.data.alerts,
                    activity
                });
            }

            return fraudAnalysis;

        } catch (error) {
            logger.error('Fraud detection failed', {
                userId,
                error: error.message
            });
            throw error;
        }
    }
}

class FinancialServicesClient {
    constructor(aiClient) {
        this.aiClient = aiClient;
        this.baseUrl = config.domains.financialServices.baseUrl;
    }

    async analyzeFinancialTransactions(userId) {
        try {
            // Get transaction history
            const transactionsResponse = await axios.get(
                `${this.baseUrl}${config.domains.financialServices.endpoints.transactions}`,
                { params: { userId, limit: 1000 } }
            );
            const transactions = transactionsResponse.data;

            // Analyze with AI
            const analysis = await this.aiClient.callService(
                'predictive-analytics',
                '/api/v1/predict',
                {
                    metric: 'financial_health',
                    historical_data: transactions,
                    confidence_level: 0.95
                }
            );

            // Detect anomalies
            const anomalyAnalysis = await this.aiClient.callService(
                'fraud-detection',
                '/api/v1/transactions/analyze',
                {
                    transactions: transactions.slice(-100), // Last 100 transactions
                    userId
                }
            );

            // Generate financial forecast
            const forecast = await this.aiClient.callService(
                'predictive-analytics',
                '/api/v1/forecast',
                {
                    data: transactions,
                    forecast_periods: 12,
                    model_type: 'prophet'
                }
            );

            const insights = {
                analysis: analysis.data,
                anomalies: anomalyAnalysis.data,
                forecast: forecast.data,
                generatedAt: new Date().toISOString()
            };

            // Store insights
            await axios.post(`${this.baseUrl}/api/v1/analytics/insights`, {
                userId,
                type: 'financial_analysis',
                insights
            });

            return insights;

        } catch (error) {
            logger.error('Financial transaction analysis failed', {
                userId,
                error: error.message
            });
            throw error;
        }
    }

    async predictMarketTrends(location, propertyType) {
        try {
            const marketTrends = await this.aiClient.callService(
                'predictive-analytics',
                '/api/v1/market-trends',
                {
                    location,
                    property_type: propertyType,
                    time_horizon: 180 // 6 months
                }
            );

            return marketTrends;

        } catch (error) {
            logger.error('Market trend prediction failed', {
                location,
                propertyType,
                error: error.message
            });
            throw error;
        }
    }
}

// Initialize clients
const aiClient = new AIGatewayClient();
const propertyClient = new PropertyManagementClient(aiClient);
const userClient = new UserManagementClient(aiClient);
const financialClient = new FinancialServicesClient(aiClient);

// Middleware
app.use(helmet());
app.use(compression());
app.use(cors({
    origin: process.env.CORS_ORIGIN || '*',
    credentials: true
}));

// Rate Limiting
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 500,
    standardHeaders: true,
    legacyHeaders: false,
});
app.use(limiter);

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Request ID middleware
app.use((req, res, next) => {
    req.id = uuidv4();
    res.setHeader('X-Request-ID', req.id);
    logger.info('Incoming request', { requestId: req.id, method: req.method, url: req.url });
    next();
});

// Validation middleware
const handleValidationErrors = (req, res, next) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({
            success: false,
            error: 'Validation failed',
            details: errors.array()
        });
    }
    next();
};

// Routes

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        service: 'management-domain-integration',
        port: config.port,
        timestamp: new Date().toISOString(),
        domains: Object.keys(config.domains)
    });
});

// Property Management Integration
app.post('/api/v1/properties/:id/enrich', [
    query('id').isUUID().withMessage('Invalid property ID')
], handleValidationErrors, async (req, res) => {
    try {
        const { id } = req.params;
        const enrichedProperty = await propertyClient.enrichPropertyWithAI(id);

        res.json({
            success: true,
            data: enrichedProperty,
            message: 'Property enriched with AI insights'
        });

    } catch (error) {
        logger.error('Property enrichment error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Property enrichment failed',
            details: error.message
        });
    }
});

app.get('/api/v1/users/:userId/recommendations', [
    query('userId').isUUID().withMessage('Invalid user ID')
], handleValidationErrors, async (req, res) => {
    try {
        const { userId } = req.params;
        const recommendations = await propertyClient.generatePropertyRecommendations(userId);

        res.json({
            success: true,
            data: recommendations
        });

    } catch (error) {
        logger.error('Recommendations error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Failed to generate recommendations',
            details: error.message
        });
    }
});

// User Management Integration
app.post('/api/v1/users/:id/analyze-behavior', [
    query('id').isUUID().withMessage('Invalid user ID')
], handleValidationErrors, async (req, res) => {
    try {
        const { id } = req.params;
        const behaviorAnalysis = await userClient.analyzeUserBehavior(id);

        res.json({
            success: true,
            data: behaviorAnalysis,
            message: 'User behavior analysis completed'
        });

    } catch (error) {
        logger.error('Behavior analysis error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Behavior analysis failed',
            details: error.message
        });
    }
});

app.post('/api/v1/users/:id/detect-fraud', [
    query('id').isUUID().withMessage('Invalid user ID'),
    body('activity').isObject().withMessage('Activity data is required')
], handleValidationErrors, async (req, res) => {
    try {
        const { id } = req.params;
        const { activity } = req.body;
        const fraudAnalysis = await userClient.detectFraudulentActivity(id, activity);

        res.json({
            success: true,
            data: fraudAnalysis
        });

    } catch (error) {
        logger.error('Fraud detection error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Fraud detection failed',
            details: error.message
        });
    }
});

// Financial Services Integration
app.post('/api/v1/users/:id/analyze-finances', [
    query('id').isUUID().withMessage('Invalid user ID')
], handleValidationErrors, async (req, res) => {
    try {
        const { id } = req.params;
        const insights = await financialClient.analyzeFinancialTransactions(id);

        res.json({
            success: true,
            data: insights,
            message: 'Financial analysis completed'
        });

    } catch (error) {
        logger.error('Financial analysis error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Financial analysis failed',
            details: error.message
        });
    }
});

app.get('/api/v1/market-trends', [
    query('location').notEmpty().withMessage('Location is required'),
    query('propertyType').optional().isString()
], handleValidationErrors, async (req, res) => {
    try {
        const { location, propertyType } = req.query;
        const trends = await financialClient.predictMarketTrends(location, propertyType);

        res.json({
            success: true,
            data: trends
        });

    } catch (error) {
        logger.error('Market trends error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Failed to fetch market trends',
            details: error.message
        });
    }
});

// Batch Processing Endpoint
app.post('/api/v1/batch/sync', [
    body('type').isIn(['properties', 'users', 'transactions']).withMessage('Invalid sync type'),
    body('batchSize').optional().isInt({ min: 1, max: 1000 }).withMessage('Invalid batch size')
], handleValidationErrors, async (req, res) => {
    try {
        const { type, batchSize = 100 } = req.body;

        let results = [];
        switch (type) {
            case 'properties':
                // Get properties to sync
                const propertiesResponse = await axios.get(
                    `${config.domains.propertyManagement.baseUrl}${config.domains.propertyManagement.endpoints.properties}`,
                    { params: { limit: batchSize, needsAIEnrichment: true } }
                );

                // Process in parallel
                const propertyPromises = propertiesResponse.data.data.map(property =>
                    propertyClient.enrichPropertyWithAI(property.id)
                );
                results = await Promise.allSettled(propertyPromises);
                break;

            case 'users':
                // Similar for users
                break;

            case 'transactions':
                // Similar for transactions
                break;
        }

        res.json({
            success: true,
            processed: results.length,
            successful: results.filter(r => r.status === 'fulfilled').length,
            failed: results.filter(r => r.status === 'rejected').length
        });

    } catch (error) {
        logger.error('Batch sync error', { error: error.message });
        res.status(500).json({
            success: false,
            error: 'Batch sync failed',
            details: error.message
        });
    }
});

// Error handling
app.use((error, req, res, next) => {
    logger.error('Unhandled error', { error: error.message });
    res.status(500).json({
        success: false,
        error: 'Internal server error'
    });
});

// Start server
const server = app.listen(config.port, () => {
    logger.info(`Management Domain Integration started on port ${config.port}`, {
        environment: config.env,
        domains: Object.keys(config.domains).length
    });

    console.log(`🔗 Management Domain Integration started on http://localhost:${config.port}`);
    console.log(`🏠 Property Integration: /api/v1/properties`);
    console.log(`👤 User Integration: /api/v1/users`);
    console.log(`💰 Financial Integration: /api/v1/users/:id/analyze-finances`);
});

module.exports = app;