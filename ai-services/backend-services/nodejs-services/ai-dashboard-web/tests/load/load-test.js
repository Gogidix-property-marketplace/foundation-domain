const http = require('http');
const { performance } = require('perf_hooks');

class LoadTest {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.results = {
      totalRequests: 0,
      successfulRequests: 0,
      failedRequests: 0,
      responseTimes: [],
      errors: []
    };
  }

  async makeRequest(path, method = 'GET', data = null) {
    return new Promise((resolve, reject) => {
      const startTime = performance.now();
      const postData = data ? JSON.stringify(data) : null;

      const options = {
        hostname: 'localhost',
        port: 3000,
        path: path,
        method: method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token'
        }
      };

      if (postData) {
        options.headers['Content-Length'] = Buffer.byteLength(postData);
      }

      const req = http.request(options, (res) => {
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', () => {
          const endTime = performance.now();
          const responseTime = endTime - startTime;

          this.results.totalRequests++;
          this.results.responseTimes.push(responseTime);

          if (res.statusCode >= 200 && res.statusCode < 400) {
            this.results.successfulRequests++;
          } else {
            this.results.failedRequests++;
            this.results.errors.push({
              statusCode: res.statusCode,
              message: body
            });
          }

          resolve({
            statusCode: res.statusCode,
            responseTime,
            body
          });
        });
      });

      req.on('error', (error) => {
        const endTime = performance.now();
        const responseTime = endTime - startTime;

        this.results.totalRequests++;
        this.results.failedRequests++;
        this.results.responseTimes.push(responseTime);
        this.results.errors.push(error);

        reject(error);
      });

      if (postData) {
        req.write(postData);
      }

      req.end();
    });
  }

  async runConcurrentRequests(path, method, data, concurrency = 10, totalRequests = 100) {
    console.log(`Running ${totalRequests} requests with ${concurrency} concurrent connections...`);

    const promises = [];
    const results = [];

    for (let i = 0; i < totalRequests; i++) {
      promises.push(
        this.makeRequest(path, method, data)
          .then(result => results.push(result))
          .catch(error => results.push({ error: error.message }))
      );

      if (promises.length >= concurrency) {
        await Promise.allSettled(promises);
        promises.length = 0;
      }
    }

    // Process remaining promises
    if (promises.length > 0) {
      await Promise.allSettled(promises);
    }

    return results;
  }

  getStats() {
    const responseTimes = this.results.responseTimes;
    const sortedTimes = responseTimes.sort((a, b) => a - b);

    return {
      totalRequests: this.results.totalRequests,
      successfulRequests: this.results.successfulRequests,
      failedRequests: this.results.failedRequests,
      successRate: (this.results.successfulRequests / this.results.totalRequests * 100).toFixed(2) + '%',
      avgResponseTime: responseTimes.length > 0
        ? (responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length).toFixed(2) + 'ms'
        : '0ms',
      minResponseTime: responseTimes.length > 0 ? Math.min(...responseTimes).toFixed(2) + 'ms' : '0ms',
      maxResponseTime: responseTimes.length > 0 ? Math.max(...responseTimes).toFixed(2) + 'ms' : '0ms',
      p50ResponseTime: sortedTimes.length > 0 ? sortedTimes[Math.floor(sortedTimes.length * 0.5)].toFixed(2) + 'ms' : '0ms',
      p95ResponseTime: sortedTimes.length > 0 ? sortedTimes[Math.floor(sortedTimes.length * 0.95)].toFixed(2) + 'ms' : '0ms',
      p99ResponseTime: sortedTimes.length > 0 ? sortedTimes[Math.floor(sortedTimes.length * 0.99)].toFixed(2) + 'ms' : '0ms',
      errors: this.results.errors.slice(0, 10) // Show first 10 errors
    };
  }

  reset() {
    this.results = {
      totalRequests: 0,
      successfulRequests: 0,
      failedRequests: 0,
      responseTimes: [],
      errors: []
    };
  }
}

// Run load tests
async function runLoadTests() {
  const loadTest = new LoadTest();

  console.log('Starting Load Tests for AI Dashboard Web Service\n');

  // Test 1: Health endpoint
  console.log('Test 1: Health Check Endpoint');
  await loadTest.runConcurrentRequests('/health', 'GET', null, 20, 50);
  let stats = loadTest.getStats();
  console.log('Health Check Results:');
  console.log(`  Total Requests: ${stats.totalRequests}`);
  console.log(`  Success Rate: ${stats.successRate}`);
  console.log(`  Avg Response Time: ${stats.avgResponseTime}`);
  console.log(`  P95 Response Time: ${stats.p95ResponseTime}\n`);

  loadTest.reset();

  // Test 2: Analytics endpoints
  console.log('Test 2: Analytics Events Endpoint');
  await loadTest.runConcurrentRequests('/api/v1/analytics/events', 'GET', null, 10, 100);
  stats = loadTest.getStats();
  console.log('Analytics Events Results:');
  console.log(`  Total Requests: ${stats.totalRequests}`);
  console.log(`  Success Rate: ${stats.successRate}`);
  console.log(`  Avg Response Time: ${stats.avgResponseTime}`);
  console.log(`  P95 Response Time: ${stats.p95ResponseTime}`);

  if (stats.errors.length > 0) {
    console.log(`  Sample Errors: ${stats.errors.slice(0, 3).map(e => e.message || e.statusCode).join(', ')}`);
  }
  console.log('');

  loadTest.reset();

  // Test 3: Create Analytics Events (Write operations)
  console.log('Test 3: Create Analytics Events (Write)');
  const eventData = {
    eventType: 'model_prediction',
    serviceName: 'dashboard',
    userId: 'load-test-user',
    properties: {
      modelId: 'test-model',
      prediction: 'positive',
      confidence: 0.95
    }
  };
  await loadTest.runConcurrentRequests('/api/v1/analytics/events', 'POST', eventData, 5, 50);
  stats = loadTest.getStats();
  console.log('Create Events Results:');
  console.log(`  Total Requests: ${stats.totalRequests}`);
  console.log(`  Success Rate: ${stats.successRate}`);
  console.log(`  Avg Response Time: ${stats.avgResponseTime}`);
  console.log(`  P95 Response Time: ${stats.p95ResponseTime}\n`);

  loadTest.reset();

  // Test 4: Dashboard Overview
  console.log('Test 4: Dashboard Overview');
  await loadTest.runConcurrentRequests('/api/v1/dashboard/overview', 'GET', null, 15, 75);
  stats = loadTest.getStats();
  console.log('Dashboard Overview Results:');
  console.log(`  Total Requests: ${stats.totalRequests}`);
  console.log(`  Success Rate: ${stats.successRate}`);
  console.log(`  Avg Response Time: ${stats.avgResponseTime}`);
  console.log(`  P95 Response Time: ${stats.p95ResponseTime}\n`);

  loadTest.reset();

  // Test 5: Metrics Endpoint
  console.log('Test 5: Metrics Summary');
  await loadTest.runConcurrentRequests('/api/v1/analytics/metrics/summary', 'GET', null, 10, 60);
  stats = loadTest.getStats();
  console.log('Metrics Summary Results:');
  console.log(`  Total Requests: ${stats.totalRequests}`);
  console.log(`  Success Rate: ${stats.successRate}`);
  console.log(`  Avg Response Time: ${stats.avgResponseTime}`);
  console.log(`  P95 Response Time: ${stats.p95ResponseTime}\n`);

  console.log('Load Tests Complete!');
}

// Export for use in other test files
module.exports = { LoadTest, runLoadTests };

// Run if called directly
if (require.main === module) {
  runLoadTests().catch(console.error);
}