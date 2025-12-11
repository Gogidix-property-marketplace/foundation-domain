const fs = require('fs');
const path = require('path');

const services = [
    'analytics-dashboard-web',
    'custom-report-builder',
    'executive-dashboard-web',
    'real-time-dashboard',
    'shared-components-web',
    'visualization-web'
];

services.forEach(service => {
    const packageJsonPath = path.join(__dirname, service, 'package.json');

    if (fs.existsSync(packageJsonPath)) {
        const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

        // Update scripts
        packageJson.scripts = {
            ...packageJson.scripts,
            'test:watch': 'jest --watch',
            'test:coverage': 'jest --coverage'
        };

        // Add supertest dependency
        if (!packageJson.devDependencies) {
            packageJson.devDependencies = {};
        }
        packageJson.devDependencies.supertest = '^6.3.3';

        // Add jest configuration
        packageJson.jest = {
            testEnvironment: 'node',
            collectCoverageFrom: [
                'src/**/*.js',
                '!src/server.js'
            ],
            coverageDirectory: 'coverage',
            coverageReporters: ['text', 'lcov', 'html'],
            testMatch: ['**/tests/**/*.test.js']
        };

        fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
        console.log(`Updated ${service}/package.json`);
    }
});

console.log('All package.json files updated successfully!');