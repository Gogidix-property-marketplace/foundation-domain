#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

class ProductionCertifier {
  constructor(serviceName, servicePath) {
    this.serviceName = serviceName;
    this.servicePath = servicePath;
    this.results = {
      passed: [],
      failed: [],
      warnings: []
    };
  }

  log(message, type = 'info') {
    const timestamp = new Date().toISOString();
    const prefix = type === 'pass' ? '✅' : type === 'fail' ? '❌' : type === 'warn' ? '⚠️' : 'ℹ️';
    console.log(`${prefix} [${timestamp}] ${message}`);
  }

  async runCommand(command, description) {
    try {
      this.log(`Running: ${description}`);
      const output = execSync(command, {
        cwd: this.servicePath,
        encoding: 'utf8',
        stdio: 'pipe'
      });
      this.log(`✓ ${description} completed successfully`, 'pass');
      this.results.passed.push(description);
      return output;
    } catch (error) {
      this.log(`✗ ${description} failed: ${error.message}`, 'fail');
      this.results.failed.push({
        test: description,
        error: error.message
      });
      return null;
    }
  }

  async checkFileExists(filePath, description) {
    const fullPath = path.join(this.servicePath, filePath);
    if (fs.existsSync(fullPath)) {
      this.log(`✓ ${description} exists`, 'pass');
      this.results.passed.push(description);
      return true;
    } else {
      this.log(`✗ ${description} missing`, 'fail');
      this.results.failed.push(description);
      return false;
    }
  }

  async checkDependencies() {
    this.log('\n=== Checking Dependencies ===');

    // Check if package.json exists
    await this.checkFileExists('package.json', 'package.json');

    // Check if node_modules exists
    await this.checkFileExists('node_modules', 'node_modules directory');

    // Check for security vulnerabilities
    await this.runCommand('npm audit --audit-level moderate', 'Security audit (npm audit)');
  }

  async checkCodeQuality() {
    this.log('\n=== Code Quality Checks ===');

    // Check if .eslintrc exists
    await this.checkFileExists('.eslintrc.js', 'ESLint configuration');

    // Run linting
    await this.runCommand('npm run lint', 'ESLint check');

    // Check for prettier configuration
    await this.checkFileExists('.prettierrc', 'Prettier configuration');
  }

  async checkTests() {
    this.log('\n=== Testing Checks ===');

    // Check test configuration
    await this.checkFileExists('jest.config.js', 'Jest configuration');

    // Check if tests directory exists
    await this.checkFileExists('tests', 'Test directory');

    // Run unit tests
    await this.runCommand('npm run test', 'Unit tests');

    // Run test coverage
    await this.runCommand('npm run test:coverage', 'Test coverage');
  }

  async checkProductionConfig() {
    this.log('\n=== Production Configuration ===');

    // Check for environment configuration
    await this.checkFileExists('.env.example', 'Environment variables example');

    // Check for production configuration file
    await this.checkFileExists('config/production.js', 'Production configuration');

    // Check for Docker configuration
    await this.checkFileExists('Dockerfile', 'Docker configuration');

    // Check for Docker Compose
    await this.checkFileExists('docker-compose.yml', 'Docker Compose configuration');
  }

  async checkSecurity() {
    this.log('\n=== Security Checks ===');

    // Check for helmet usage
    const packageJson = require(path.join(this.servicePath, 'package.json'));
    if (packageJson.dependencies && packageJson.dependencies.helmet) {
      this.log('✓ Helmet security middleware included', 'pass');
      this.results.passed.push('Helmet security middleware');
    } else {
      this.log('⚠️ Helmet security middleware not found', 'warn');
      this.results.warnings.push('Consider adding Helmet for security headers');
    }

    // Check for rate limiting
    if (packageJson.dependencies && packageJson.dependencies['express-rate-limit']) {
      this.log('✓ Rate limiting middleware included', 'pass');
      this.results.passed.push('Rate limiting middleware');
    } else {
      this.log('⚠️ Rate limiting middleware not found', 'warn');
      this.results.warnings.push('Consider adding rate limiting');
    }

    // Check for CORS configuration
    if (packageJson.dependencies && packageJson.dependencies.cors) {
      this.log('✓ CORS middleware included', 'pass');
      this.results.passed.push('CORS middleware');
    } else {
      this.log('⚠️ CORS middleware not found', 'warn');
      this.results.warnings.push('Consider adding CORS configuration');
    }
  }

  async checkMonitoring() {
    this.log('\n=== Monitoring & Logging ===');

    // Check for Winston logging
    const packageJson = require(path.join(this.servicePath, 'package.json'));
    if (packageJson.dependencies && packageJson.dependencies.winston) {
      this.log('✓ Winston logger included', 'pass');
      this.results.passed.push('Winston logger');
    } else {
      this.log('⚠️ Winston logger not found', 'warn');
      this.results.warnings.push('Consider adding Winston for structured logging');
    }

    // Check for health endpoint
    const appFile = path.join(this.servicePath, 'src/app.js');
    if (fs.existsSync(appFile)) {
      const appContent = fs.readFileSync(appFile, 'utf8');
      if (appContent.includes('/health')) {
        this.log('✓ Health endpoint configured', 'pass');
        this.results.passed.push('Health endpoint');
      } else {
        this.log('⚠️ Health endpoint not found', 'warn');
        this.results.warnings.push('Consider adding a health check endpoint');
      }
    }

    // Check for metrics/prometheus
    if (packageJson.dependencies && packageJson.dependencies['prom-client']) {
      this.log('✓ Prometheus metrics included', 'pass');
      this.results.passed.push('Prometheus metrics');
    }
  }

  async checkAPI() {
    this.log('\n=== API Checks ===');

    // Check for API documentation
    await this.checkFileExists('src/routes', 'API routes directory');

    // Check for Swagger/OpenAPI
    const packageJson = require(path.join(this.servicePath, 'package.json'));
    if (packageJson.dependencies &&
        (packageJson.dependencies['swagger-jsdoc'] ||
         packageJson.dependencies['swagger-ui-express'])) {
      this.log('✓ API documentation tools included', 'pass');
      this.results.passed.push('API documentation');
    } else {
      this.log('⚠️ API documentation tools not found', 'warn');
      this.results.warnings.push('Consider adding Swagger/OpenAPI documentation');
    }
  }

  async checkCI_CD() {
    this.log('\n=== CI/CD Configuration ===');

    // Check for GitHub Actions
    await this.checkFileExists('.github/workflows', 'GitHub Actions workflows');

    // Check for Jenkinsfile
    await this.checkFileExists('Jenkinsfile', 'Jenkins pipeline configuration');

    // Check for deployment scripts
    await this.checkFileExists('scripts', 'Deployment scripts directory');
  }

  generateReport() {
    this.log('\n' + '='.repeat(50));
    this.log('PRODUCTION READINESS CERTIFICATION REPORT');
    this.log('=' .repeat(50));
    this.log(`\nService: ${this.serviceName}`);
    this.log(`Timestamp: ${new Date().toISOString()}`);

    this.log(`\n✅ PASSED CHECKS (${this.results.passed.length}):`);
    this.results.passed.forEach(check => this.log(`  ✓ ${check}`));

    if (this.results.warnings.length > 0) {
      this.log(`\n⚠️ WARNINGS (${this.results.warnings.length}):`);
      this.results.warnings.forEach(warning => this.log(`  ⚠ ${warning}`));
    }

    if (this.results.failed.length > 0) {
      this.log(`\n❌ FAILED CHECKS (${this.results.failed.length}):`);
      this.results.failed.forEach(failure => {
        if (typeof failure === 'object') {
          this.log(`  ❌ ${failure.test}: ${failure.error}`);
        } else {
          this.log(`  ❌ ${failure}`);
        }
      });
    }

    const total = this.results.passed.length + this.results.failed.length;
    const passRate = total > 0 ? ((this.results.passed.length / total) * 100).toFixed(2) : 0;

    this.log(`\n📊 SUMMARY:`);
    this.log(`  Total Checks: ${total}`);
    this.log(`  Passed: ${this.results.passed.length}`);
    this.log(`  Failed: ${this.results.failed.length}`);
    this.log(`  Warnings: ${this.results.warnings.length}`);
    this.log(`  Pass Rate: ${passRate}%`);

    let certification = 'NOT CERTIFIED';
    if (passRate >= 95 && this.results.failed.length === 0) {
      certification = 'FULLY CERTIFIED ✅';
    } else if (passRate >= 85 && this.results.failed.length <= 2) {
      certification = 'CONDITIONALLY CERTIFIED ⚠️';
    } else if (passRate >= 70) {
      certification = 'REQUIRES IMPROVEMENT 🔄';
    }

    this.log(`\n🏆 CERTIFICATION STATUS: ${certification}`);

    // Save report to file
    const reportPath = path.join(this.servicePath, `production-certification-${Date.now()}.json`);
    fs.writeFileSync(reportPath, JSON.stringify({
      serviceName: this.serviceName,
      timestamp: new Date().toISOString(),
      results: this.results,
      passRate,
      certification
    }, null, 2));

    this.log(`\n📄 Report saved to: ${reportPath}`);

    return {
      passed: this.results.passed.length,
      failed: this.results.failed.length,
      warnings: this.results.warnings.length,
      passRate: parseFloat(passRate),
      certification
    };
  }

  async certify() {
    this.log(`🚀 Starting Production Certification for ${this.serviceName}`);
    this.log('='.repeat(50));

    try {
      await this.checkDependencies();
      await this.checkCodeQuality();
      await this.checkTests();
      await this.checkProductionConfig();
      await this.checkSecurity();
      await this.checkMonitoring();
      await this.checkAPI();
      await this.checkCI_CD();

      return this.generateReport();
    } catch (error) {
      this.log(`Certification failed with error: ${error.message}`, 'fail');
      throw error;
    }
  }
}

// Run certification if called directly
if (require.main === module) {
  const serviceName = process.argv[2] || 'ai-dashboard-web';
  const servicePath = process.argv[3] || process.cwd();

  const certifier = new ProductionCertifier(serviceName, servicePath);
  certifier.certify()
    .then(result => {
      process.exit(result.passRate >= 85 ? 0 : 1);
    })
    .catch(error => {
      console.error('Certification failed:', error);
      process.exit(1);
    });
}

module.exports = ProductionCertifier;