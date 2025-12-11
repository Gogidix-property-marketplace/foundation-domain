# ai-risk-assessment-service

## Overview
Gogidix AI Service for risk assessment

## Technology Stack
- **Java 21** - Latest LTS version
- **Spring Boot 3.2.2** - Modern Spring framework
- **Spring Security 6** - Security features
- **Spring Data JPA** - Database access
- **PostgreSQL** - Production database
- **H2 Database** - Development database
- **Maven** - Build tool
- **Docker** - Containerization

## Features
- Hexagonal Architecture (Ports & Adapters)
- Domain-Driven Design (DDD)
- RESTful API endpoints
- Comprehensive error handling
- Input validation
- Security authentication & authorization
- Health checks and monitoring
- Circuit breaker pattern
- Comprehensive unit and integration tests

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 13+ (for production)

### Running Locally
```bash
# Clone the repository
git clone <repository-url>
cd ai-risk-assessment-service

# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

### Docker
```bash
# Build Docker image
docker build -t ai-risk-assessment-service:1.0.0 .

# Run container
docker run -p 8233:8080 ai-risk-assessment-service:1.0.0
```

## API Documentation
Once the application is running, visit:
- Swagger UI: http://localhost:8233/swagger-ui.html
- Health Check: http://localhost:8233/api/v1/actuator/health
- Metrics: http://localhost:8233/api/v1/actuator/metrics

## Configuration
Configuration is managed through `application.yml` files:
- `application.yml` - Default configuration
- `application-dev.yml` - Development profile
- `application-prod.yml` - Production profile

## Environment Variables
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `SPRING_PROFILES_ACTIVE` - Active Spring profile

## Testing
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Generate test coverage report
mvn jacoco:report
```

## Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License
This project is licensed under the MIT License.

## Support
For questions and support, please contact the Gogidix AI Team.
