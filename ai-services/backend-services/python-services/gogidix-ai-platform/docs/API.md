# Gogidix AI Services API Documentation

## Table of Contents
- [Authentication](#authentication)
- [AI Gateway API](#ai-gateway-api)
- [Property Intelligence API](#property-intelligence-api)
- [Conversational AI API](#conversational-ai-api)
- [Analytics API](#analytics-api)
- [ML Platform API](#ml-platform-api)
- [Ethical AI API](#ethical-ai-api)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)

## Base URL
```
Production: https://api.gogidix.com
Staging: https://staging-api.gogidix.com
Development: http://localhost:8000
```

## Authentication

All API requests require authentication using JWT tokens or API keys.

### JWT Token Authentication
```http
Authorization: Bearer <your_jwt_token>
```

### API Key Authentication
```http
X-API-Key: <your_api_key>
```

### Service-to-Service Authentication
```http
X-Service-Token: <your_service_token>
X-Service-Name: <service_name>
```

## AI Gateway API

### Model List
```http
GET /api/v1/models
```

**Response:**
```json
{
  "models": [
    {
      "id": "property-valuation-v2",
      "name": "Property Valuation Model",
      "version": "2.0.0",
      "type": "regression",
      "status": "active",
      "created_at": "2024-01-15T10:30:00Z",
      "updated_at": "2024-01-20T14:22:00Z"
    }
  ],
  "total": 1
}
```

### Model Inference
```http
POST /api/v1/models/{model_id}/predict
Content-Type: application/json

{
  "input": {
    "property_type": "apartment",
    "bedrooms": 3,
    "bathrooms": 2,
    "square_feet": 1200,
    "location": "Manhattan, NY",
    "year_built": 2010
  },
  "parameters": {
    "return_explanation": true,
    "confidence_threshold": 0.8
  }
}
```

**Response:**
```json
{
  "prediction": 850000,
  "confidence": 0.92,
  "explanation": {
    "feature_importance": {
      "location": 0.35,
      "square_feet": 0.28,
      "bedrooms": 0.15,
      "year_built": 0.12,
      "property_type": 0.10
    }
  },
  "model_version": "2.0.0",
  "timestamp": "2024-01-25T10:30:00Z"
}
```

## Property Intelligence API

### Property Valuation
```http
POST /api/v1/property-intelligence/valuation
Content-Type: application/json
Authorization: Bearer <token>

{
  "property": {
    "address": "123 Main St, New York, NY 10001",
    "type": "apartment",
    "bedrooms": 3,
    "bathrooms": 2,
    "square_feet": 1200,
    "year_built": 2010,
    "features": [
      "balcony",
      "doorman",
      "gym"
    ],
    "coordinates": {
      "latitude": 40.7484,
      "longitude": -73.9857
    }
  },
  "market_data": {
    "comparable_properties": true,
    "neighborhood_trends": true,
    "price_history": true
  }
}
```

**Response:**
```json
{
  "valuation": {
    "estimated_value": 850000,
    "price_per_sqft": 708,
    "confidence_interval": [800000, 900000],
    "market_position": "above_average"
  },
  "comparables": [
    {
      "address": "125 Main St",
      "price": 825000,
      "square_feet": 1150,
      "similarity_score": 0.92
    }
  ],
  "market_trends": {
    "neighborhood appreciation": "5.2%",
    "days_on_market": 28,
    "inventory_level": "low"
  },
  "timestamp": "2024-01-25T10:30:00Z"
}
```

### Image Analysis
```http
POST /api/v1/property-intelligence/analyze-image
Content-Type: multipart/form-data

file: <image_file>
property_id: property_123
analysis_types: ["room_detection", "quality_assessment", "feature_extraction"]
```

**Response:**
```json
{
  "analysis_id": "analysis_456",
  "property_id": "property_123",
  "results": {
    "room_detection": {
      "rooms": [
        {
          "type": "living_room",
          "confidence": 0.95,
          "area": "45%",
          "features": ["large_window", "hardwood_floor"]
        },
        {
          "type": "kitchen",
          "confidence": 0.88,
          "area": "20%",
          "features": ["modern_appliances", "granite_countertops"]
        }
      ]
    },
    "quality_assessment": {
      "overall_score": 8.5,
      "condition": "excellent",
      "factors": {
        "cleanliness": 9.0,
        "maintenance": 8.2,
        "modernization": 8.8
      }
    },
    "feature_extraction": {
      "amenities": ["balcony", "in_unit_laundry", "dishwasher"],
      "style": "modern",
      "natural_light": "excellent"
    }
  },
  "processing_time": 2.3,
  "timestamp": "2024-01-25T10:30:00Z"
}
```

### Market Analysis
```http
GET /api/v1/property-intelligence/market-analysis
?location=Manhattan,NY
&property_type=apartment
&bedrooms_min=2
&bedrooms_max=3
&time_range=6m

Authorization: Bearer <token>
```

**Response:**
```json
{
  "location": "Manhattan, NY",
  "analysis_period": "6 months",
  "market_summary": {
    "average_price": 920000,
    "price_per_sqft": 950,
    "days_on_market": 35,
    "inventory_count": 1250,
    "price_trend": "increasing",
    "appreciation_rate": "4.8%"
  },
  "price_distribution": {
    "median": 875000,
    "q1": 650000,
    "q3": 1250000,
    "outliers": {
      "high": 3500000,
      "low": 450000
    }
  },
  "neighborhoods": [
    {
      "name": "Upper East Side",
      "average_price": 1100000,
      "appreciation": "5.2%",
      "inventory": 180
    }
  ],
  "timestamp": "2024-01-25T10:30:00Z"
}
```

## Conversational AI API

### Chat with AI
```http
POST /api/v1/conversational-ai/chat
Content-Type: application/json
Authorization: Bearer <token>

{
  "message": "What are the best neighborhoods in NYC for families?",
  "conversation_id": "conv_123",
  "user_id": "user_456",
  "context": {
    "preferences": {
      "property_type": "apartment",
      "budget_range": [500000, 1000000],
      "bedrooms": 3
    }
  },
  "parameters": {
    "language": "en",
    "temperature": 0.7,
    "include_suggestions": true
  }
}
```

**Response:**
```json
{
  "response": "Based on your preferences, I'd recommend considering these neighborhoods for families...",
  "conversation_id": "conv_123",
  "message_id": "msg_789",
  "intent": "neighborhood_recommendation",
  "entities": [
    {"type": "location", "value": "NYC", "confidence": 0.95},
    {"type": "family_preference", "value": true, "confidence": 0.88}
  ],
  "suggestions": [
    {
      "type": "property_search",
      "title": "View 3-bedroom apartments in recommended areas",
      "action": "/search?bedrooms=3&areas=park_slope,forest_hills"
    }
  ],
  "timestamp": "2024-01-25T10:30:00Z"
}
```

### Streaming Chat
```http
POST /api/v1/conversational-ai/chat/stream
Content-Type: application/json
Accept: text/event-stream

{
  "message": "Tell me about the current real estate market trends",
  "conversation_id": "conv_123",
  "stream": true
}
```

**Response (Server-Sent Events):**
```
event: message
data: {"type": "token", "content": "The current real"}

event: message
data: {"type": "token", "content": " estate market shows"}

event: message
data: {"type": "complete", "response": "..."}
```

## Analytics API

### Property Analytics Dashboard
```http
GET /api/v1/analytics/dashboard
?date_range=30d
&metrics=[listings,views,inquiries]
&granularity=daily

Authorization: Bearer <token>
```

**Response:**
```json
{
  "dashboard_data": {
    "overview": {
      "total_listings": 15420,
      "total_views": 892340,
      "total_inquiries": 12580,
      "conversion_rate": 0.014
    },
    "trends": [
      {"date": "2024-01-01", "listings": 485, "views": 28920, "inquiries": 412},
      {"date": "2024-01-02", "listings": 502, "views": 31250, "inquiries": 438}
    ],
    "top_locations": [
      {"location": "Manhattan", "listings": 3240, "avg_price": 1250000},
      {"location": "Brooklyn", "listings": 2890, "avg_price": 850000}
    ]
  }
}
```

### User Behavior Analytics
```http
POST /api/v1/analytics/track
Content-Type: application/json

{
  "event": "property_view",
  "user_id": "user_123",
  "session_id": "session_456",
  "properties": {
    "property_id": "prop_789",
    "source": "search_results",
    "duration": 45,
    "interaction": "photo_gallery"
  }
}
```

## ML Platform API

### Model Training Job
```http
POST /api/v1/ml-platform/train
Content-Type: application/json

{
  "model_type": "property_valuation",
  "dataset": {
    "source": "s3://gogidix-data/training/property_data.parquet",
    "features": ["square_feet", "bedrooms", "location", "year_built"],
    "target": "price",
    "test_split": 0.2
  },
  "hyperparameters": {
    "model": "xgboost",
    "learning_rate": 0.01,
    "max_depth": 6,
    "n_estimators": 1000
  },
  "resources": {
    "gpu": true,
    "cpu": 4,
    "memory": "16Gi"
  }
}
```

**Response:**
```json
{
  "job_id": "job_123",
  "status": "queued",
  "estimated_duration": "2 hours",
  "callback_url": "/api/v1/ml-platform/jobs/job_123/status"
}
```

### Model Deployment
```http
POST /api/v1/ml-platform/deploy
Content-Type: application/json

{
  "model_id": "model_456",
  "deployment_config": {
    "replicas": 3,
    "autoscaling": {
      "min_replicas": 2,
      "max_replicas": 10,
      "target_cpu": 70
    },
    "resources": {
      "cpu": "1",
      "memory": "2Gi",
      "gpu": "0.5"
    }
  }
}
```

## Ethical AI API

### Bias Assessment
```http
POST /api/v1/ethical-ai/bias-detection
Content-Type: application/json

{
  "model_id": "model_123",
  "dataset_path": "s3://gogidix-data/bias_test.parquet",
  "sensitive_attributes": ["gender", "race", "age"],
  "bias_types": ["demographic_parity", "equalized_odds"]
}
```

**Response:**
```json
{
  "assessment_id": "assessment_456",
  "model_id": "model_123",
  "bias_results": [
    {
      "bias_type": "demographic_parity",
      "metric_value": 0.08,
      "threshold": 0.1,
      "is_biased": false,
      "severity": "low"
    }
  ],
  "overall_bias_score": 92.0,
  "recommendations": ["Model shows low bias across all attributes"]
}
```

### Model Explainability
```http
POST /api/v1/ethical-ai/explainability
Content-Type: application/json

{
  "model_id": "model_123",
  "input_data": {
    "property_type": "apartment",
    "square_feet": 1200,
    "location": "Manhattan"
  },
  "explanation_methods": ["shap", "lime"]
}
```

**Response:**
```json
{
  "explanation_id": "exp_789",
  "model_id": "model_123",
  "feature_importance": {
    "location": 0.45,
    "square_feet": 0.32,
    "property_type": 0.23
  },
  "shap_values": {
    "location": 150000,
    "square_feet": 80000,
    "property_type": -50000
  },
  "confidence_score": 0.87
}
```

## Error Handling

All errors follow this standard format:

```json
{
  "error": "ValidationError",
  "message": "Invalid input parameters",
  "error_code": "AI_003",
  "status_code": 400,
  "details": [
    {
      "code": "INVALID_VALUE",
      "message": "square_feet must be positive",
      "field": "square_feet"
    }
  ],
  "timestamp": "2024-01-25T10:30:00Z",
  "request_id": "req_123"
}
```

### Error Codes
- `AI_001`: Model not found
- `AI_002`: Model prediction failed
- `AI_003`: Validation error
- `AI_004`: Authentication error
- `AI_005`: Authorization error
- `AI_006`: Rate limit exceeded
- `AI_007`: Resource exhausted
- `AI_008`: Service unavailable
- `AI_009`: Timeout error
- `AI_010`: Configuration error

## Rate Limiting

API requests are rate limited based on your subscription plan:

| Plan | Requests/Minute | Requests/Hour | Burst |
|------|----------------|---------------|-------|
| Free | 60 | 1000 | 10 |
| Basic | 500 | 10000 | 50 |
| Pro | 2000 | 50000 | 200 |
| Enterprise | 10000 | 1000000 | 1000 |

Rate limit headers are included in every response:
```http
X-RateLimit-Limit: 5000
X-RateLimit-Remaining: 4999
X-RateLimit-Reset: 1643103600
```

## SDKs

### Python SDK
```python
from gogidix_ai import GogidixAI

# Initialize client
client = GogidixAI(api_key="your_api_key")

# Property valuation
result = client.property_intelligence.valuate(
    property_type="apartment",
    bedrooms=3,
    location="Manhattan, NY"
)

print(result.predicted_price)
```

### JavaScript SDK
```javascript
import { GogidixAI } from '@gogidix/ai-sdk';

const client = new GogidixAI({ apiKey: 'your_api_key' });

const prediction = await client.property.valuation({
  property: {
    type: 'apartment',
    bedrooms: 3,
    location: 'Manhattan, NY'
  }
});
```

## Webhooks

Configure webhooks to receive real-time notifications:

### Create Webhook
```http
POST /api/v1/webhooks
Content-Type: application/json

{
  "url": "https://your-app.com/webhooks/gogidix",
  "events": ["model_trained", "model_deployed", "bias_detected"],
  "secret": "webhook_secret"
}
```

### Webhook Payload
```json
{
  "event": "model_trained",
  "timestamp": "2024-01-25T10:30:00Z",
  "data": {
    "model_id": "model_123",
    "accuracy": 0.92,
    "job_id": "job_456"
  },
  "signature": "sha256=..."
}
```