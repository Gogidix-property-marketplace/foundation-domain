# BLUEPRINT GUIDELINES - API GATEWAY REFERENCE

## Gold Standard Blueprint Location

**Primary Reference**:
```
C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\java-services\api-gateway
```

## AI Services Integration Architecture

### 1. Service Communication Patterns
The API Gateway demonstrates patterns for AI service integration:
- **RESTful API Design**: Consistent endpoint structure for AI services
- **Authentication Flow**: JWT validation for AI service access
- **Rate Limiting**: Request throttling for expensive AI operations
- **Circuit Breaking**: Failover for AI model availability
- **Load Balancing**: Distributing AI inference requests

### 2. AI Service Gateway Architecture
Follow gateway's service mesh patterns:
```python
# AI Gateway service following API Gateway patterns
class AIGatewayService:
    def __init__(self):
        self.model_router = ModelRouter()
        self.rate_limiter = RateLimiter()
        self.auth_service = AuthService()
        self.monitoring = MonitoringService()

    async def process_ai_request(self, request: AIRequest) -> AIResponse:
        # Authentication following gateway patterns
        if not await self.auth_service.validate_token(request.token):
            raise UnauthorizedError("Invalid authentication token")

        # Rate limiting for expensive AI operations
        if not await self.rate_limiter.check_limit(request.user_id):
            raise RateLimitError("Rate limit exceeded")

        # Route to appropriate AI model
        return await self.model_router.route(request)
```

### 3. Configuration Management
Mirror API Gateway's configuration approach:
```yaml
# AI Service configuration following gateway patterns
ai:
  gateway:
    timeout: 30000  # 30 seconds for AI operations
    rate-limit:
      requests-per-minute: 100
      burst-capacity: 20
    models:
      property-valuation:
        endpoint: "http://property-valuation-service:8080"
        model-version: "v2.1"
        timeout: 15000
      image-recognition:
        endpoint: "http://image-recognition-service:8080"
        model-version: "v1.5"
        timeout: 5000
      chatbot:
        endpoint: "http://chatbot-service:8080"
        model-version: "v3.0"
        timeout: 10000
```

## AI Service Implementation Guidelines

### Model Serving Architecture
Follow API Gateway's microservices patterns:
```python
# AI Model Service structure
class AIModelService:
    """Base class for AI model services following gateway patterns"""

    def __init__(self, model_path: str, config: Dict):
        self.model = self.load_model(model_path)
        self.config = config
        self.metrics = PrometheusMetrics()
        self.logger = StructuredLogger()

    async def predict(self, input_data: Any) -> Prediction:
        """Prediction with monitoring and error handling"""
        with self.metrics.timer('prediction_duration'):
            try:
                # Input validation following gateway patterns
                validated_input = self.validate_input(input_data)

                # Model inference
                prediction = await self.model.predict(validated_input)

                # Post-processing
                result = self.post_process(prediction)

                # Metrics and logging
                self.metrics.increment('predictions_total')
                self.logger.info('Prediction completed', {
                    'model_version': self.config['version'],
                    'input_hash': hash(str(input_data))
                })

                return result

            except Exception as e:
                self.metrics.increment('prediction_errors')
                self.logger.error('Prediction failed', {'error': str(e)})
                raise AIServiceError(f"Model prediction failed: {str(e)}")
```

### API Design Standards
Follow OpenAPI 3.0 patterns from API Gateway:
```python
# AI Service API endpoints following gateway patterns
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel

app = FastAPI(title="AI Services API", version="1.0.0")

class PropertyValuationRequest(BaseModel):
    property_id: str
    features: Dict[str, Any]
    location: LocationData
    images: List[str] = []

class PropertyValuationResponse(BaseModel):
    estimated_value: float
    confidence_score: float
    valuation_date: datetime
    market_comparison: Dict[str, Any]
    metadata: Dict[str, Any]

@app.post("/api/v1/ai/property-valuation")
async def property_valuation(
    request: PropertyValuationRequest,
    current_user: User = Depends(get_current_user)
) -> PropertyValuationResponse:
    """Property valuation endpoint following gateway patterns"""

    # Rate limiting check
    if not await rate_limiter.check_limit(current_user.id):
        raise HTTPException(status_code=429, detail="Rate limit exceeded")

    # Validate input following gateway validation patterns
    if not validator.validate_property_request(request):
        raise HTTPException(status_code=400, detail="Invalid request data")

    # Process through AI model
    try:
        result = await property_valuation_model.predict(request)
        return PropertyValuationResponse(**result)
    except Exception as e:
        logger.error(f"Property valuation failed: {str(e)}")
        raise HTTPException(status_code=500, detail="AI service unavailable")
```

## Data Flow and Processing Guidelines

### Request Processing Pipeline
Follow API Gateway's request processing patterns:
```python
class AIRequestProcessor:
    """Request processing following API Gateway patterns"""

    def __init__(self):
        self.auth_service = AuthService()
        self.validator = RequestValidator()
        self.rate_limiter = RateLimiter()
        self.ai_service_router = AIServiceRouter()
        self.response_formatter = ResponseFormatter()

    async def process_request(self, request: HTTPRequest) -> HTTPResponse:
        """Complete request processing pipeline"""

        # 1. Authentication (following gateway patterns)
        auth_result = await self.auth_service.authenticate(request)
        if not auth_result.success:
            return self.create_error_response(401, auth_result.error)

        # 2. Request validation
        validation_result = await self.validator.validate(request)
        if not validation_result.valid:
            return self.create_error_response(400, validation_result.errors)

        # 3. Rate limiting
        if not await self.rate_limiter.check_limit(auth_result.user_id):
            return self.create_error_response(429, "Rate limit exceeded")

        # 4. Route to AI service
        ai_response = await self.ai_service_router.route(request)

        # 5. Format response (following gateway response patterns)
        formatted_response = await self.response_formatter.format(ai_response)

        return formatted_response
```

### Error Handling Standards
Mirror API Gateway's error handling approach:
```python
class AIErrorHandler:
    """Error handling following API Gateway patterns"""

    ERROR_RESPONSES = {
        "MODEL_UNAVAILABLE": {
            "status": 503,
            "code": "AI_001",
            "message": "AI model temporarily unavailable",
            "retry_after": 30
        },
        "INVALID_INPUT": {
            "status": 400,
            "code": "AI_002",
            "message": "Invalid input data provided",
            "details": None
        },
        "QUOTA_EXCEEDED": {
            "status": 429,
            "code": "AI_003",
            "message": "AI service quota exceeded",
            "retry_after": 3600
        }
    }

    def handle_error(self, error: Exception) -> HTTPResponse:
        """Standardized error response following gateway patterns"""

        if isinstance(error, ModelNotFoundError):
            return self.create_error_response(self.ERROR_RESPONSES["MODEL_UNAVAILABLE"])
        elif isinstance(error, ValidationError):
            return self.create_error_response({
                **self.ERROR_RESPONSES["INVALID_INPUT"],
                "details": error.validation_errors
            })
        elif isinstance(error, QuotaExceededError):
            return self.create_error_response(self.ERROR_RESPONSES["QUOTA_EXCEEDED"])
        else:
            # Unknown error - following gateway security patterns
            logger.error(f"Unknown AI service error: {str(error)}")
            return self.create_error_response({
                "status": 500,
                "code": "AI_999",
                "message": "Internal AI service error"
            })
```

## Monitoring and Observability Guidelines

### Metrics Collection
Follow API Gateway's monitoring patterns:
```python
from prometheus_client import Counter, Histogram, Gauge

# AI Service metrics following gateway patterns
ai_requests_total = Counter(
    'ai_requests_total',
    'Total AI service requests',
    ['service', 'model', 'status']
)

ai_request_duration = Histogram(
    'ai_request_duration_seconds',
    'AI request processing time',
    ['service', 'model'],
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0]
)

ai_model_accuracy = Gauge(
    'ai_model_accuracy',
    'AI model accuracy score',
    ['service', 'model', 'version']
)

ai_gpu_utilization = Gauge(
    'ai_gpu_utilization_percent',
    'GPU utilization percentage',
    ['service', 'gpu_id']
)

class AIMetricsCollector:
    """Metrics collection following API Gateway patterns"""

    @staticmethod
    def record_prediction(service: str, model: str, duration: float, status: str):
        ai_requests_total.labels(
            service=service,
            model=model,
            status=status
        ).inc()
        ai_request_duration.labels(
            service=service,
            model=model
        ).observe(duration)

    @staticmethod
    def update_model_accuracy(service: str, model: str, version: str, accuracy: float):
        ai_model_accuracy.labels(
            service=service,
            model=model,
            version=version
        ).set(accuracy)
```

### Logging Standards
Follow API Gateway's structured logging approach:
```python
import structlog

logger = structlog.get_logger()

class AILogger:
    """Structured logging following API Gateway patterns"""

    @staticmethod
    def log_prediction_request(
        service: str,
        model: str,
        user_id: str,
        request_id: str,
        input_size: int
    ):
        logger.info(
            "AI prediction request received",
            service=service,
            model=model,
            user_id=user_id,
            request_id=request_id,
            input_size=input_size,
            timestamp=datetime.utcnow().isoformat()
        )

    @staticmethod
    def log_model_performance(
        service: str,
        model: str,
        version: str,
        accuracy: float,
        latency: float,
        throughput: float
    ):
        logger.info(
            "Model performance metrics",
            service=service,
            model=model,
            version=version,
            accuracy=accuracy,
            latency=latency,
            throughput=throughput,
            timestamp=datetime.utcnow().isoformat()
        )
```

## Security Integration Guidelines

### Authentication and Authorization
Follow API Gateway's security patterns:
```python
# AI Service authentication following gateway patterns
from functools import wraps

def require_ai_access(permission: str):
    """Decorator for AI service access control"""
    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # Extract token from request (following gateway patterns)
            token = request.headers.get('Authorization')
            if not token:
                raise UnauthorizedError("Missing authentication token")

            # Validate token with gateway auth service
            user_info = await auth_service.validate_token(token)
            if not user_info:
                raise UnauthorizedError("Invalid authentication token")

            # Check AI service specific permissions
            if not has_ai_permission(user_info, permission):
                raise ForbiddenError("Insufficient AI service permissions")

            # Add user context to request
            request.user = user_info
            return await func(*args, **kwargs)
        return wrapper
    return decorator

# Usage example
@app.post("/api/v1/ai/chat")
@require_ai_access("chatbot:use")
async def chat_endpoint(request: ChatRequest):
    """Chatbot endpoint with gateway-style authentication"""
    return await chatbot_service.process_message(request.message, request.user.id)
```

### Input Validation and Sanitization
Follow API Gateway's security validation:
```python
class AIInputValidator:
    """Input validation following API Gateway security patterns"""

    @staticmethod
    def validate_text_input(text: str, max_length: int = 10000) -> str:
        """Validate and sanitize text input"""
        if not text or not text.strip():
            raise ValidationError("Text input cannot be empty")

        if len(text) > max_length:
            raise ValidationError(f"Text input exceeds maximum length of {max_length}")

        # Sanitize input (following gateway security patterns)
        sanitized = html.escape(text.strip())

        # Check for malicious patterns
        malicious_patterns = [
            r'<script.*?>.*?</script>',
            r'javascript:',
            r'on\w+\s*=',
        ]

        for pattern in malicious_patterns:
            if re.search(pattern, sanitized, re.IGNORECASE):
                raise ValidationError("Potentially malicious content detected")

        return sanitized

    @staticmethod
    def validate_image_data(image_data: bytes, max_size: int = 10 * 1024 * 1024):
        """Validate image data"""
        if len(image_data) > max_size:
            raise ValidationError("Image size exceeds maximum limit")

        # Validate image format
        try:
            img = Image.open(io.BytesIO(image_data))
            if img.format not in ['JPEG', 'PNG', 'WEBP']:
                raise ValidationError("Unsupported image format")
        except Exception:
            raise ValidationError("Invalid image data")

        return image_data
```

## Performance Optimization Guidelines

### Caching Strategy
Follow API Gateway's caching patterns:
```python
class AIModelCache:
    """Model result caching following gateway patterns"""

    def __init__(self, redis_client):
        self.redis = redis_client
        self.cache_ttl = 3600  # 1 hour cache

    async def get_cached_result(self, cache_key: str) -> Optional[Any]:
        """Get cached AI result"""
        try:
            cached_data = await self.redis.get(cache_key)
            if cached_data:
                return json.loads(cached_data)
        except Exception as e:
            logger.warning(f"Cache retrieval failed: {str(e)}")
        return None

    async def cache_result(self, cache_key: str, result: Any, ttl: int = None):
        """Cache AI result"""
        try:
            ttl = ttl or self.cache_ttl
            await self.redis.setex(
                cache_key,
                ttl,
                json.dumps(result, default=str)
            )
        except Exception as e:
            logger.warning(f"Cache storage failed: {str(e)}")

    def generate_cache_key(self, service: str, model: str, input_hash: str) -> str:
        """Generate cache key following gateway patterns"""
        return f"ai:{service}:{model}:{input_hash}"

# Usage in AI services
@cached_result(ttl=1800)  # 30 minutes cache
async def property_valuation_cached(property_data: Dict) -> Dict:
    """Cached property valuation"""
    return await property_valuation_model.predict(property_data)
```

### Batch Processing Optimization
```python
class AIBatchProcessor:
    """Batch processing following gateway optimization patterns"""

    def __init__(self, batch_size: int = 32, max_wait_time: float = 0.1):
        self.batch_size = batch_size
        self.max_wait_time = max_wait_time
        self.request_queue = asyncio.Queue()
        self.results = {}

    async def process_batch(self, requests: List[AIRequest]) -> List[AIResponse]:
        """Process multiple AI requests efficiently"""
        try:
            # Batch preprocessing
            batch_inputs = [req.input_data for req in requests]

            # Model batch inference
            batch_results = await self.model.predict_batch(batch_inputs)

            # Individual result formatting
            responses = []
            for i, request in enumerate(requests):
                response = AIResponse(
                    request_id=request.request_id,
                    result=batch_results[i],
                    processing_time=request.processing_time
                )
                responses.append(response)

            return responses

        except Exception as e:
            logger.error(f"Batch processing failed: {str(e)}")
            # Fallback to individual processing
            return await self.process_individually(requests)
```

## Testing Guidelines

### Model Testing
Follow API Gateway's comprehensive testing approach:
```python
class AIModelTestSuite:
    """Testing suite following gateway testing patterns"""

    def test_model_accuracy(self, test_data: List[Dict], threshold: float = 0.9):
        """Test model accuracy against threshold"""
        predictions = []
        actuals = []

        for data in test_data:
            prediction = self.model.predict(data['input'])
            predictions.append(prediction)
            actuals.append(data['expected'])

        accuracy = self.calculate_accuracy(predictions, actuals)
        assert accuracy >= threshold, f"Model accuracy {accuracy} below threshold {threshold}"

    def test_inference_time(self, test_data: List[Dict], max_time: float = 1.0):
        """Test inference time meets requirements"""
        for data in test_data:
            start_time = time.time()
            self.model.predict(data['input'])
            inference_time = time.time() - start_time
            assert inference_time <= max_time, f"Inference time {inference_time} exceeds limit {max_time}"

    def test_input_robustness(self, invalid_inputs: List[Any]):
        """Test model handles invalid inputs gracefully"""
        for invalid_input in invalid_inputs:
            try:
                result = self.model.predict(invalid_input)
                # Should either handle gracefully or raise appropriate error
            except Exception as e:
                assert isinstance(e, (ValidationError, InvalidInputError)), \
                    f"Unexpected error type for invalid input: {type(e)}"
```

### Integration Testing
```python
class AIServiceIntegrationTest:
    """Integration testing following API Gateway patterns"""

    async def test_ai_gateway_routing(self):
        """Test request routing through AI gateway"""
        # Test different model routing
        valuation_request = AIRequest(service="property-valuation", data=sample_property_data)
        response = await ai_gateway.process_request(valuation_request)
        assert response.service == "property-valuation"
        assert isinstance(response.result, PropertyValuation)

    async def test_rate_limiting(self):
        """Test rate limiting enforcement"""
        user_id = "test_user"

        # Send requests up to limit
        for _ in range(rate_limit):
            response = await ai_gateway.process_request(create_test_request(user_id))
            assert response.status_code == 200

        # Next request should be rate limited
        response = await ai_gateway.process_request(create_test_request(user_id))
        assert response.status_code == 429

    async def test_authentication(self):
        """Test authentication requirements"""
        # Request without token should fail
        request = create_test_request_without_auth()
        response = await ai_gateway.process_request(request)
        assert response.status_code == 401

        # Request with valid token should succeed
        request = create_test_request_with_valid_auth()
        response = await ai_gateway.process_request(request)
        assert response.status_code == 200
```

---

**Reference Document**: API Gateway Implementation
**Last Updated**: 2025-11-28
**Review Frequency**: Monthly
**Approval Required**: CTO Architecture Review