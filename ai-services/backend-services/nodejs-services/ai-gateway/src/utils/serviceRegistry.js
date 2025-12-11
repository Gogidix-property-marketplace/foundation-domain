const axios = require('axios');
const logger = require('./logger');

const services = [
  {
    name: 'dashboard',
    url: 'http://localhost:3000',
    port: 3000,
    path: '/health',
    critical: true
  },
  {
    name: 'training',
    url: 'http://localhost:3001',
    port: 3001,
    path: '/health',
    critical: true
  },
  {
    name: 'vision',
    url: 'http://localhost:3002',
    port: 3002,
    path: '/health',
    critical: false
  },
  {
    name: 'data-quality',
    url: 'http://localhost:3003',
    port: 3003,
    path: '/health',
    critical: false
  },
  {
    name: 'documents',
    url: 'http://localhost:3004',
    port: 3004,
    path: '/health',
    critical: false
  },
  {
    name: 'models',
    url: 'http://localhost:3005',
    port: 3005,
    path: '/health',
    critical: true
  },
  {
    name: 'nlp',
    url: 'http://localhost:3006',
    port: 3006,
    path: '/health',
    critical: false
  }
];

/**
 * Check health of a single service
 * @param {Object} service - Service configuration
 * @returns {Promise<Object>} Health status of the service
 */
async function checkServiceHealth(service) {
  const startTime = Date.now();

  try {
    const response = await axios.get(
      `${service.url}${service.path}`,
      {
        timeout: 5000, // 5 second timeout
        validateStatus: (status) => status < 500
      }
    );

    const responseTime = Date.now() - startTime;

    return {
      status: response.data.status === 'OK' || response.data.status === 'healthy' ? 'healthy' : 'unhealthy',
      responseTime,
      lastCheck: new Date().toISOString(),
      uptime: response.data.uptime || null,
      memory: response.data.memory || null,
      details: response.data
    };
  } catch (error) {
    const responseTime = Date.now() - startTime;

    return {
      status: 'unhealthy',
      responseTime,
      lastCheck: new Date().toISOString(),
      error: error.message,
      details: {
        code: error.code,
        status: error.response?.status,
        statusText: error.response?.statusText
      }
    };
  }
}

/**
 * Get health status of all services
 * @returns {Promise<Object>} Health status of all services
 */
async function getServiceHealth() {
  const healthStatuses = {};

  // Check all services in parallel
  const healthPromises = services.map(async (service) => {
    const health = await checkServiceHealth(service);
    return { name: service.name, health, critical: service.critical };
  });

  const results = await Promise.allSettled(healthPromises);

  // Process results
  results.forEach((result, index) => {
    const service = services[index];
    if (result.status === 'fulfilled') {
      healthStatuses[service.name] = result.value;
    } else {
      healthStatuses[service.name] = {
        status: 'unhealthy',
        responseTime: 5000,
        lastCheck: new Date().toISOString(),
        error: result.reason.message
      };
    }
  });

  return healthStatuses;
}

/**
 * Get service URL by name
 * @param {string} name - Service name
 * @returns {string|null} Service URL
 */
function getServiceUrl(name) {
  const service = services.find(s => s.name === name);
  return service ? service.url : null;
}

/**
 * Get all service URLs
 * @returns {Object} Map of service names to URLs
 */
function getAllServiceUrls() {
  return services.reduce((acc, service) => {
    acc[service.name] = service.url;
    return acc;
  }, {});
}

/**
 * Register a new service
 * @param {Object} serviceConfig - Service configuration
 * @param {string} serviceConfig.name - Service name
 * @param {string} serviceConfig.url - Service URL
 * @param {number} serviceConfig.port - Service port
 * @param {string} serviceConfig.path - Health check path
 * @param {boolean} serviceConfig.critical - Whether service is critical
 */
function registerService(serviceConfig) {
  const existingIndex = services.findIndex(s => s.name === serviceConfig.name);

  if (existingIndex >= 0) {
    services[existingIndex] = serviceConfig;
  } else {
    services.push(serviceConfig);
  }

  logger.info(`Service ${serviceConfig.name} registered at ${serviceConfig.url}`);
}

/**
 * Unregister a service
 * @param {string} name - Service name to unregister
 */
function unregisterService(name) {
  const index = services.findIndex(s => s.name === name);

  if (index >= 0) {
    services.splice(index, 1);
    logger.info(`Service ${name} unregistered`);
  }
}

/**
 * Get all registered services
 * @returns {Array} Array of registered services
 */
function getAllServices() {
  return [...services];
}

/**
 * Get critical services
 * @returns {Array} Array of critical services
 */
function getCriticalServices() {
  return services.filter(s => s.critical);
}

module.exports = {
  getServiceHealth,
  getServiceUrl,
  getAllServiceUrls,
  registerService,
  unregisterService,
  getAllServices,
  getCriticalServices
};