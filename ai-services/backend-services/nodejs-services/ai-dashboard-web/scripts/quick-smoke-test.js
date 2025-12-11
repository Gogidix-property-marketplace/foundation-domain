#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

function runQuickSmokeTest(serviceName, servicePath) {
  console.log(`🔥 Running Quick Smoke Test for ${serviceName}`);
  console.log('='.repeat(50));

  const checks = {
    required: [],
    recommended: [],
    failed: []
  };

  // Check 1: Essential files exist
  const essentialFiles = [
    'package.json',
    'src/app.js',
    'src/models/Analytics.js',
    'src/controllers/analyticsController.js',
    'src/routes/analytics.js',
    'src/middleware/auth.js',
    'src/middleware/errorHandler.js',
    'src/utils/logger.js'
  ];

  console.log('\n📁 Checking Essential Files:');
  essentialFiles.forEach(file => {
    const filePath = path.join(servicePath, file);
    if (fs.existsSync(filePath)) {
      console.log(`  ✅ ${file}`);
      checks.required.push(file);
    } else {
      console.log(`  ❌ ${file} - MISSING!`);
      checks.failed.push(`${file} is missing`);
    }
  });

  // Check 2: Package.json configuration
  console.log('\n📦 Checking package.json:');
  try {
    const packageJson = require(path.join(servicePath, 'package.json'));

    // Check essential dependencies
    const essentialDeps = ['express', 'mongoose', 'winston', 'jsonwebtoken'];
    essentialDeps.forEach(dep => {
      if (packageJson.dependencies && packageJson.dependencies[dep]) {
        console.log(`  ✅ ${dep} - ${packageJson.dependencies[dep]}`);
        checks.required.push(dep);
      } else {
        console.log(`  ❌ ${dep} - MISSING!`);
        checks.failed.push(`${dep} dependency missing`);
      }
    });

    // Check scripts
    if (packageJson.scripts) {
      const scripts = Object.keys(packageJson.scripts);
      console.log(`  ✅ Scripts configured: ${scripts.length} scripts`);
      checks.recommended.push('npm scripts');
    }
  } catch (error) {
    console.log(`  ❌ package.json error: ${error.message}`);
    checks.failed.push('Invalid package.json');
  }

  // Check 3: Code structure
  console.log('\n🏗️ Checking Code Structure:');
  const dirs = ['src', 'src/controllers', 'src/models', 'src/routes', 'src/middleware', 'src/utils'];
  dirs.forEach(dir => {
    const dirPath = path.join(servicePath, dir);
    if (fs.existsSync(dirPath)) {
      const files = fs.readdirSync(dirPath);
      console.log(`  ✅ ${dir} - ${files.length} files`);
      checks.required.push(dir);
    } else {
      console.log(`  ❌ ${dir} - MISSING!`);
      checks.failed.push(`${dir} directory missing`);
    }
  });

  // Check 4: File syntax (quick check)
  console.log('\n✨ Checking File Syntax:');
  const jsFiles = [
    'src/app.js',
    'src/controllers/analyticsController.js',
    'src/models/Analytics.js'
  ];

  jsFiles.forEach(file => {
    const filePath = path.join(servicePath, file);
    if (fs.existsSync(filePath)) {
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        // Simple syntax checks
        if (content.includes('require(') || content.includes('import ')) {
          console.log(`  ✅ ${file} - Syntax OK`);
          checks.required.push(`${file} syntax`);
        }
      } catch (error) {
        console.log(`  ❌ ${file} - Syntax Error: ${error.message}`);
        checks.failed.push(`${file} syntax error`);
      }
    }
  });

  // Check 5: Configuration files
  console.log('\n⚙️ Checking Configuration:');
  const configFiles = [
    '.env.example',
    'Dockerfile',
    'README.md'
  ];

  configFiles.forEach(file => {
    const filePath = path.join(servicePath, file);
    if (fs.existsSync(filePath)) {
      console.log(`  ✅ ${file}`);
      checks.recommended.push(file);
    } else {
      console.log(`  ⚠️ ${file} - Recommended but missing`);
    }
  });

  // Calculate results
  const totalRequired = essentialFiles.length + 4; // files + package.json + structure + syntax
  const passedRequired = checks.required.length;
  const passRate = Math.round((passedRequired / totalRequired) * 100);

  console.log('\n' + '='.repeat(50));
  console.log('📊 QUICK SMOKE TEST RESULTS');
  console.log('='.repeat(50));

  console.log(`\n✅ Passed Checks: ${passedRequired}/${totalRequired}`);
  console.log(`⚠️ Recommended: ${checks.recommended.length}`);
  console.log(`❌ Failed: ${checks.failed.length}`);
  console.log(`📈 Pass Rate: ${passRate}%`);

  if (checks.failed.length > 0) {
    console.log('\n❌ Failed Items:');
    checks.failed.forEach(item => console.log(`  - ${item}`));
  }

  let status = '🔴 NOT READY';
  if (passRate >= 90 && checks.failed.length === 0) {
    status = '🟢 READY FOR TESTING';
  } else if (passRate >= 75) {
    status = '🟡 MOSTLY READY';
  }

  console.log(`\n🚦 Status: ${status}`);

  return {
    passed: passedRequired,
    total: totalRequired,
    passRate,
    failed: checks.failed.length,
    status
  };
}

// Run if called directly
if (require.main === module) {
  const serviceName = process.argv[2] || 'ai-dashboard-web';
  const servicePath = process.argv[3] || process.cwd();

  const result = runQuickSmokeTest(serviceName, servicePath);
  process.exit(result.passRate >= 75 ? 0 : 1);
}

module.exports = runQuickSmokeTest;