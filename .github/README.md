# Foundation Domain CI/CD Documentation

## Overview

This document describes the CI/CD pipeline setup for the Gogidix Foundation Domain, which automates the build, test, and deployment of 306+ microservices across 5 domains.

## Architecture

### Pipeline Components

1. **CI Pipeline** (`ci-foundation-domain.yml`)
   - Triggered on push to main/develop branches
   - Parallel builds for Java, Node.js, and Python services
   - Change detection to optimize build times
   - Automated testing and security scanning
   - Deployment to staging environment

2. **Production Deployment** (`deploy-production.yml`)
   - Manual approval required
   - Wave-based deployment strategy
   - Comprehensive health checks
   - Post-deployment monitoring

### Supported Technologies

| Technology | Version | Build Tool | Test Framework |
|------------|---------|------------|----------------|
| Java | 21 | Maven | JUnit 5, TestContainers |
| Node.js | 20 | npm | Jest |
| Python | 3.11 | pip | pytest |

## Workflow Triggers

### On Push
- Builds all modified services
- Runs unit and integration tests
- Deploys to staging (develop branch only)
- Performs security scanning

### On Pull Request
- Builds changed services
- Runs all tests
- Performs security scanning
- No deployment

### Manual Production Deployment
- Requires approval
- Wave-based deployment to minimize risk
- Comprehensive health checks
- Deployment tagging

## Environment Configuration

### Required Secrets

Add these secrets to your GitHub repository:

1. `KUBE_CONFIG`
   - Base64 encoded Kubernetes config for staging

2. `KUBE_CONFIG_PROD`
   - Base64 encoded Kubernetes config for production

3. `SLACK_WEBHOOK_URL`
   - Slack webhook for deployment notifications

4. `SONAR_TOKEN` (optional)
   - For code quality analysis

### Environment Variables

- `REGISTRY`: Container registry (default: ghcr.io)
- `JAVA_VERSION`: Java version (default: 21)
- `NODE_VERSION`: Node.js version (default: 20)
- `PYTHON_VERSION`: Python version (default: 3.11)

## Deployment Strategy

### Staging Environment
- Automatic deployment on develop branch
- Full test suite execution
- Integration testing
- Performance monitoring

### Production Environment
- Manual approval through GitHub environments
- Wave-based deployment:
  1. Wave 1: Shared Infrastructure (critical services)
  2. Wave 2: Central Configuration
  3. Wave 3a: AI Services (batch 1 - core services)
  4. Wave 3b: AI Services (batch 2 - remaining services)
  5. Wave 4: Centralized Dashboard

### Rollback Strategy

Each deployment creates a Git tag for easy rollback:

```bash
# View deployment tags
git tag --list "production-deploy-*"

# Rollback to previous deployment
git checkout <previous-tag>
```

## Monitoring and Observability

### Health Checks
- Spring Boot Actuator endpoints for Java services
- Custom health endpoints for Node.js/Python services
- Kubernetes readiness and liveness probes

### Notifications
- Slack integration for deployment status
- Email alerts for critical failures
- GitHub Issues auto-creation for failed deployments

## Best Practices

### For Developers

1. **Service Structure**
   - Follow the established directory structure
   - Include Dockerfile in each service
   - Add appropriate health endpoints

2. **Testing**
   - Unit tests are mandatory
   - Integration tests recommended
   - Add health check verification

3. **Commit Messages**
   - Follow conventional commit format
   - Include affected domain in commit
   - Reference relevant issues

### For Operations

1. **Monitoring**
   - Monitor pod resource usage
   - Check service response times
   - Watch error rates

2. **Scaling**
   - Configure HPA for stateless services
   - Set resource requests and limits
   - Monitor cluster capacity

## Troubleshooting

### Common Issues

1. **Build Failures**
   - Check Maven/Node.js dependencies
   - Verify syntax and imports
   - Review test failures

2. **Deployment Issues**
   - Verify Kubernetes manifests
   - Check resource limits
   - Review service dependencies

3. **Health Check Failures**
   - Verify service startup sequence
   - Check database connections
   - Review configuration values

### Debugging Commands

```bash
# Check deployment status
kubectl get deployments -n foundation-staging
kubectl describe deployment <service-name> -n foundation-staging

# Check pod logs
kubectl logs -f <pod-name> -n foundation-staging

# Check events
kubectl get events -n foundation-staging --sort-by='.lastTimestamp'
```

## Security Considerations

1. **Image Scanning**
   - Trivy vulnerability scanning on all images
   - Results uploaded to GitHub Security tab

2. **Secrets Management**
   - Never commit secrets to repository
   - Use Kubernetes secrets
   - Rotate credentials regularly

3. **Network Security**
   - Network policies implemented
   - Service mesh integration (Istio) ready
   - TLS encryption for all communications

## Performance Optimization

1. **Build Optimization**
   - Maven dependency caching
   - Docker layer caching
   - Parallel builds

2. **Deployment Optimization**
   - Rolling updates with zero downtime
   - Resource-aware scheduling
   - Pod disruption budgets

## Future Enhancements

1. **Advanced Deployments**
   - Canary deployments with ArgoCD
   - Blue-green deployments
   - Feature flags integration

2. **Enhanced Monitoring**
   - Distributed tracing with Jaeger
   - Metrics with Prometheus
   - Logging with ELK stack

3. **Automation**
   - Automated security patches
   - Dependency updates
   - Performance regression testing