#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

class JavaTemplateProcessor {
  constructor() {
    this.services = [
      {
        name: 'ai-anomaly-detection-service',
        serviceName: 'AnomalyDetection',
        entityName: 'Transaction',
        domainName: 'anomaly',
        packageName: 'com.gogidix.ai.anomaly',
        port: 8081,
        path: 'backend-services/java-services/ai-anomaly-detection-service'
      },
      {
        name: 'ai-automated-tagging-service',
        serviceName: 'AutomatedTagging',
        entityName: 'Content',
        domainName: 'tagging',
        packageName: 'com.gogidix.ai.tagging',
        port: 8082,
        path: 'backend-services/java-services/ai-automated-tagging-service'
      },
      {
        name: 'ai-bi-analytics-service',
        serviceName: 'BIAnalytics',
        entityName: 'Insight',
        domainName: 'analytics',
        packageName: 'com.gogidix.ai.analytics',
        port: 8083,
        path: 'backend-services/java-services/ai-bi-analytics-service'
      },
      {
        name: 'ai-categorization-service',
        serviceName: 'Categorization',
        entityName: 'Item',
        domainName: 'category',
        packageName: 'com.gogidix.ai.category',
        port: 8084,
        path: 'backend-services/java-services/ai-categorization-service'
      },
      {
        name: 'ai-chatbot-service',
        serviceName: 'Chatbot',
        entityName: 'Conversation',
        domainName: 'chat',
        packageName: 'com.gogidix.ai.chat',
        port: 8085,
        path: 'backend-services/java-services/ai-chatbot-service'
      },
      {
        name: 'ai-computer-vision-service',
        serviceName: 'ComputerVision',
        entityName: 'Image',
        domainName: 'vision',
        packageName: 'com.gogidix.ai.vision',
        port: 8086,
        path: 'backend-services/java-services/ai-computer-vision-service'
      },
      {
        name: 'ai-content-moderation-service',
        serviceName: 'ContentModeration',
        entityName: 'Content',
        domainName: 'moderation',
        packageName: 'com.gogidix.ai.moderation',
        port: 8087,
        path: 'backend-services/java-services/ai-content-moderation-service'
      },
      {
        name: 'ai-data-quality-service',
        serviceName: 'DataQuality',
        entityName: 'Validation',
        domainName: 'dataquality',
        packageName: 'com.gogidix.ai.dataquality',
        port: 8088,
        path: 'backend-services/java-services/ai-data-quality-service'
      },
      {
        name: 'ai-forecasting-service',
        serviceName: 'Forecasting',
        entityName: 'Prediction',
        domainName: 'forecast',
        packageName: 'com.gogidix.ai.forecast',
        port: 8089,
        path: 'backend-services/java-services/ai-forecasting-service'
      },
      {
        name: 'ai-fraud-detection-service',
        serviceName: 'FraudDetection',
        entityName: 'Transaction',
        domainName: 'fraud',
        packageName: 'com.gogidix.ai.fraud',
        port: 8090,
        path: 'backend-services/java-services/ai-fraud-detection-service'
      }
    ];
  }

  log(message, type = 'info') {
    const timestamp = new Date().toISOString();
    const prefix = type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️';
    console.log(`${prefix} [${timestamp}] ${message}`);
  }

  processTemplate(content, config) {
    return content
      // Basic replacements
      .replace(/\{\{projectName\}\}/g, config.name.replace('-', ''))
      .replace(/\{\{Entity\}\}/g, config.entityName)
      .replace(/\{\{DomainName\}\}/g, config.domainName)

      // Advanced replacements
      .replace(/\{\{domain\}\}/g, config.domainName)
      .replace(/\{\{Service\}\}/g, config.serviceName)
      .replace(/\{\{UseCase\}\}/g, `Process${config.entityName}`)

      // Package name updates
      .replace(/com\.gogidix\.ai\.\{\{[^}]+\}\}/g, config.packageName);
  }

  renameTemplateFile(filePath, config) {
    const parts = filePath.split('/');
    const fileName = parts[parts.length - 1];

    if (fileName.includes('{{') && fileName.includes('}}')) {
      parts[parts.length - 1] = this.processTemplate(fileName, config);
      return parts.join('/');
    }
    return filePath;
  }

  async processService(config, basePath) {
    this.log(`Processing service: ${config.name}`);

    const servicePath = path.join(basePath, config.path);

    if (!fs.existsSync(servicePath)) {
      this.log(`Service path not found: ${servicePath}`, 'error');
      return false;
    }

    // Find all Java files with placeholders
    const templateFiles = this.findTemplateFiles(servicePath);

    if (templateFiles.length === 0) {
      this.log(`No template files found in ${config.name}`, 'warning');
      return true;
    }

    this.log(`Found ${templateFiles.length} template files to process`);

    // Process each template file
    for (const filePath of templateFiles) {
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        const processedContent = this.processTemplate(content, config);
        const newPath = this.renameTemplateFile(filePath, config);

        // Write processed content
        fs.writeFileSync(newPath, processedContent);

        // If file was renamed, remove original
        if (newPath !== filePath) {
          fs.unlinkSync(filePath);
        }

        this.log(`  Processed: ${path.relative(servicePath, newPath)}`);
      } catch (error) {
        this.log(`Error processing ${filePath}: ${error.message}`, 'error');
      }
    }

    // Generate missing essential files
    await this.generateEssentialFiles(config, servicePath);

    return true;
  }

  findTemplateFiles(dir) {
    const files = [];

    function traverse(currentDir) {
      const items = fs.readdirSync(currentDir);

      for (const item of items) {
        const fullPath = path.join(currentDir, item);
        const stat = fs.statSync(fullPath);

        if (stat.isDirectory()) {
          traverse(fullPath);
        } else if (item.endsWith('.java') || item.endsWith('.xml') || item.endsWith('.yml')) {
          // Check if file contains placeholders
          try {
            const content = fs.readFileSync(fullPath, 'utf8');
            if (content.includes('{{') && content.includes('}}')) {
              files.push(fullPath);
            }
          } catch (error) {
            // Skip files that can't be read
          }
        }
      }
    }

    traverse(dir);
    return files;
  }

  async generateEssentialFiles(config, servicePath) {
    // Generate MainApplication.java if not exists
    const mainAppPath = path.join(servicePath, `${config.serviceName}Application.java`);

    if (!fs.existsSync(mainAppPath)) {
      const mainAppContent = `package ${config.packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableCaching
public class ${config.serviceName}Application {
    public static void main(String[] args) {
        SpringApplication.run(${config.serviceName}Application.class, args);
    }
}
`;
      fs.writeFileSync(mainAppPath, mainAppContent);
      this.log(`  Generated: ${config.serviceName}Application.java`);
    }

    // Generate application.yml
    const appConfigPath = path.join(servicePath, 'src/main/resources/application.yml');

    if (!fs.existsSync(appConfigPath)) {
      const appConfigContent = `server:
  port: ${config.port}
  servlet:
    context-path: /api/v1/${config.domainName}

spring:
  application:
    name: ${config.name}
  profiles:
    active: dev

  datasource:
    url: jdbc:postgresql://localhost:5432/gogidix_${config.domainName}
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  redis:
    host: localhost
    port: 6379
    timeout: 2000ms

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ${config.name}-group
      auto-offset-reset: earliest

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    ${config.packageName}: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Service-specific configuration
${config.domainName}:
  max-concurrent-requests: 100
  request-timeout: 5000
  batch-size: 1000
`;
      fs.writeFileSync(appConfigPath, appConfigContent);
      this.log(`  Generated: application.yml`);
    }

    // Generate REST Controller template
    const controllerDir = path.join(servicePath, 'src/main/java/com/gogidix/ai', config.domainName, 'controller');
    if (!fs.existsSync(controllerDir)) {
      fs.mkdirSync(controllerDir, { recursive: true });
    }

    const controllerPath = path.join(controllerDir, `${config.entityName}Controller.java`);
    if (!fs.existsSync(controllerPath)) {
      const controllerContent = `package ${config.packageName}.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ${config.packageName}.service.${config.entityName}Service;
import ${config.packageName}.dto.${config.entityName}DTO;
import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/${config.domainName}")
@RequiredArgsConstructor
public class ${config.entityName}Controller {

    private final ${config.entityName}Service ${config.domainName}Service;

    @PostMapping
    public ResponseEntity<${config.entityName}DTO> create${config.entityName}(@Valid @RequestBody ${config.entityName}DTO dto) {
        log.info("Creating ${config.domainName}: {}", dto);
        ${config.entityName}DTO result = ${config.domainName}Service.create(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<${config.entityName}DTO> get${config.entityName}(@PathVariable Long id) {
        log.info("Fetching ${config.domainName} with id: {}", id);
        return ${config.domainName}Service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<${config.entityName}DTO>> getAll${config.entityName}s() {
        log.info("Fetching all ${config.domainName}s");
        List<${config.entityName}DTO> results = ${config.domainName}Service.findAll();
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{id}")
    public ResponseEntity<${config.entityName}DTO> update${config.entityName}(
            @PathVariable Long id,
            @Valid @RequestBody ${config.entityName}DTO dto) {
        log.info("Updating ${config.domainName} with id: {}", id);
        ${config.entityName}DTO result = ${config.domainName}Service.update(id, dto);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete${config.entityName}(@PathVariable Long id) {
        log.info("Deleting ${config.domainName} with id: {}", id);
        ${config.domainName}Service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
`;
      fs.writeFileSync(controllerPath, controllerContent);
      this.log(`  Generated: ${config.entityName}Controller.java`);
    }
  }

  async processAllServices(basePath) {
    this.log('Starting Java Template Processing');
    this.log('='.repeat(50));

    const results = {
      processed: [],
      failed: [],
      skipped: []
    };

    for (const config of this.services) {
      try {
        const success = await this.processService(config, basePath);
        if (success) {
          results.processed.push(config.name);
          this.log(`✓ Successfully processed ${config.name}`, 'success');
        } else {
          results.failed.push(config.name);
        }
      } catch (error) {
        results.failed.push(config.name);
        this.log(`Failed to process ${config.name}: ${error.message}`, 'error');
      }
    }

    // Print summary
    this.log('\n' + '='.repeat(50));
    this.log('PROCESSING SUMMARY');
    this.log('='.repeat(50));
    this.log(`✓ Processed: ${results.processed.length}`);
    this.log(`✗ Failed: ${results.failed.length}`);
    this.log(`⚠ Skipped: ${results.skipped.length}`);

    if (results.processed.length > 0) {
      this.log('\n✅ Successfully Processed:');
      results.processed.forEach(service => this.log(`  - ${service}`));
    }

    if (results.failed.length > 0) {
      this.log('\n❌ Failed to Process:');
      results.failed.forEach(service => this.log(`  - ${service}`));
    }

    return results;
  }
}

// Run if called directly
if (require.main === module) {
  const basePath = process.argv[2] || process.cwd();
  const processor = new JavaTemplateProcessor();

  processor.processAllServices(basePath)
    .then(results => {
      process.exit(results.failed.length === 0 ? 0 : 1);
    })
    .catch(error => {
      console.error('Template processing failed:', error);
      process.exit(1);
    });
}

module.exports = JavaTemplateProcessor;