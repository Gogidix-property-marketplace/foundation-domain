const swaggerJsdoc = require('swagger-jsdoc');
const swaggerUi = require('swagger-ui-express');

const swaggerOptions = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Gogidix AI Services Gateway API',
      version: '1.0.0',
      description: 'API Gateway for Gogidix AI Services platform',
      contact: {
        name: 'Gogidix AI Team',
        email: 'ai-team@gogidix.com',
        url: 'https://gogidix.com'
      },
      license: {
        name: 'MIT',
        url: 'https://opensource.org/licenses/MIT'
      }
    },
    servers: [
      {
        url: process.env.API_BASE_URL || 'http://localhost:8000',
        description: 'Development server'
      },
      {
        url: 'https://api.gogidix.com/ai-gateway',
        description: 'Production server'
      }
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          description: 'Enter JWT token (without "Bearer " prefix)'
        }
      },
      schemas: {
        Error: {
          type: 'object',
          properties: {
            error: {
              type: 'string',
              description: 'Error type'
            },
            message: {
              type: 'string',
              description: 'Error message'
            },
            timestamp: {
              type: 'string',
              format: 'date-time',
              description: 'Error timestamp'
            },
            path: {
              type: 'string',
              description: 'Request path'
            }
          },
          required: ['error', 'message']
        },
        HealthStatus: {
          type: 'object',
          properties: {
            status: {
              type: 'string',
              enum: ['healthy', 'degraded', 'unhealthy'],
              description: 'Overall health status'
            },
            timestamp: {
              type: 'string',
              format: 'date-time'
            },
            gateway: {
              type: 'object',
              properties: {
                status: {
                  type: 'string'
                },
                uptime: {
                  type: 'number',
                  description: 'Uptime in seconds'
                },
                memory: {
                  type: 'object',
                  properties: {
                    rss: {
                      type: 'number',
                      description: 'Resident Set Size in bytes'
                    },
                    heapUsed: {
                      type: 'number',
                      description: 'Heap memory used in bytes'
                    }
                  }
                }
              }
            },
            services: {
              type: 'object',
              additionalProperties: {
                $ref: '#/components/schemas/ServiceHealth'
              }
            }
          }
        },
        ServiceHealth: {
          type: 'object',
          properties: {
            status: {
              type: 'string',
              enum: ['healthy', 'unhealthy']
            },
            responseTime: {
              type: 'number',
              description: 'Response time in milliseconds'
            },
            lastCheck: {
              type: 'string',
              format: 'date-time'
            },
            uptime: {
              type: 'number',
              description: 'Service uptime in seconds'
            },
            error: {
              type: 'string',
              description: 'Error message if service is unhealthy'
            }
          }
        },
        ServiceMetrics: {
          type: 'object',
          properties: {
            service: {
              type: 'string',
              description: 'Service name'
            },
            totalRequests: {
              type: 'number',
              description: 'Total number of requests'
            },
            averageResponseTime: {
              type: 'number',
              description: 'Average response time in seconds'
            },
            errorRate: {
              type: 'number',
              description: 'Error rate as percentage'
            },
            requestsPerSecond: {
              type: 'number',
              description: 'Requests per second'
            }
          }
        }
      },
      tags: [
        {
          name: 'Gateway',
          description: 'Gateway operations'
        },
        {
          name: 'Health',
          description: 'Health check endpoints'
        },
        {
          name: 'Metrics',
          description: 'Metrics and monitoring'
        },
        {
          name: 'AI Services',
          description: 'AI service proxies'
        }
      ]
    }
  },
  apis: [
    './src/routes/*.js',
    './src/config/swagger.json'
  ]
};

const swaggerSpec = swaggerJsdoc(swaggerOptions);

/**
 * Setup Swagger documentation
 * @param {Object} app - Express app
 */
function setupSwagger(app) {
  // Serve Swagger UI
  app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec, {
    explorer: true,
    customCss: '.swagger-ui .topbar { display: none }',
    customSiteTitle: 'Gogidix AI Services Gateway API Documentation'
  }));

  // Serve API spec as JSON
  app.get('/api-docs.json', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.send(swaggerSpec);
  });

  // Add custom middleware to add security requirement to endpoints that need it
  swaggerSpec.paths = swaggerSpec.paths || {};
  Object.keys(swaggerSpec.paths).forEach(path => {
    const pathItem = swaggerSpec.paths[path];
    Object.keys(pathItem).forEach(method => {
      const operation = pathItem[method];
      if (operation && (path.startsWith('/api/v1/') || method === 'post' || method === 'put' || method === 'delete')) {
        operation.security = [{ bearerAuth: [] }];
      }
    });
  });
}

module.exports = setupSwagger;