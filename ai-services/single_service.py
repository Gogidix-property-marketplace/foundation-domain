#!/usr/bin/env python3
"""
Single All-in-One AI Service
Combines all AI services into one FastAPI app for easy testing
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, Any, List, Optional
import uvicorn
import sys
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

# Create the app
app = FastAPI(
    title="Gogidix AI Services",
    description="All-in-One AI Services for Property Marketplace",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==================== MODELS ====================
class PropertyValuationRequest(BaseModel):
    property_type: str
    bedrooms: int
    bathrooms: float
    square_feet: int
    city: str
    state: Optional[str] = None
    year_built: Optional[int] = None

class ChatRequest(BaseModel):
    message: str
    conversation_id: Optional[str] = None
    language: Optional[str] = "en"

class PropertyAnalysisRequest(BaseModel):
    property_id: str
    features: List[str]

# ==================== HEALTH ENDPOINTS ====================
@app.get("/")
async def root():
    return {
        "message": "🚀 Gogidix AI Services",
        "version": "1.0.0",
        "services": ["Property Intelligence", "Conversational AI", "Analytics"],
        "status": "All services running in single instance"
    }

@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "service": "All-in-One AI Services",
        "timestamp": "2024-01-25T12:00:00Z"
    }

@app.get("/api/v1/health")
async def api_health():
    return {"status": "ok"}

# ==================== AI GATEWAY ENDPOINTS ====================
@app.get("/info")
async def info():
    return {
        "name": "Gogidix AI Services",
        "version": "1.0.0",
        "environment": "development",
        "services": {
            "ai_gateway": "✅ Running",
            "property_intelligence": "✅ Running",
            "conversational_ai": "✅ Running",
            "analytics": "✅ Running"
        }
    }

# ==================== PROPERTY INTELLIGENCE ENDPOINTS ====================
@app.post("/api/v1/property-valuation")
async def property_valuation(request: PropertyValuationRequest):
    """Estimate property value using AI."""

    # Simple valuation logic (in production, use ML model)
    base_prices = {
        "apartment": 350000,
        "house": 500000,
        "condo": 400000,
        "townhouse": 450000
    }

    price_per_sqft = {
        "New York": 1000,
        "Los Angeles": 600,
        "Chicago": 300,
        "Houston": 200,
        "Phoenix": 250
    }

    base_price = base_prices.get(request.property_type, 400000)
    city_multiplier = price_per_sqft.get(request.city, 400) / 400

    estimated_price = base_price * city_multiplier * (request.square_feet / 1000)

    # Add bedroom/bathroom adjustments
    estimated_price *= (1 + (request.bedrooms * 0.1))
    estimated_price *= (1 + (request.bathrooms * 0.05))

    return {
        "property_id": f"PROP_{hash(str(request)) % 1000000:06d}",
        "estimated_value": round(estimated_price, 2),
        "confidence_score": 0.95,
        "price_per_sqft": round(estimated_price / request.square_feet, 2),
        "comparable_properties": 3,
        "market_trend": "increasing",
        "last_updated": "2024-01-25T12:00:00Z"
    }

@app.get("/api/v1/properties/{property_id}")
async def get_property(property_id: str):
    """Get property details."""
    return {
        "property_id": property_id,
        "property_type": "apartment",
        "address": "123 Main St, New York, NY",
        "bedrooms": 2,
        "bathrooms": 2,
        "square_feet": 1200,
        "year_built": 2010,
        "estimated_value": 750000,
        "features": ["central_air", "hardwood_floors", "doorman"],
        "images": [
            f"https://images.gogidix.com/properties/{property_id}/1.jpg",
            f"https://images.gogidix.com/properties/{property_id}/2.jpg"
        ]
    }

# ==================== CONVERSATIONAL AI ENDPOINTS ====================
@app.post("/api/v1/chat")
async def chat(request: ChatRequest):
    """Chat with AI assistant."""

    responses = {
        "find apartment": "I found several apartments in your area. Would you like to see 2-bedroom options under $500,000?",
        "property value": "To get an accurate property valuation, please provide the property type, square footage, and location.",
        "best neighborhoods": "The best neighborhoods depend on your preferences. For families, I recommend areas with good schools. For young professionals, areas with nightlife and public transport.",
        "market trends": "Currently, the property market is showing steady growth with an average increase of 5% year-over-year.",
        "investment tips": "Consider factors like location, school districts, and future development plans when investing in property."
    }

    # Simple keyword matching
    message_lower = request.message.lower()
    response = "I'm here to help with your property needs. You can ask about property valuations, market trends, or investment advice."

    for keyword, reply in responses.items():
        if keyword in message_lower:
            response = reply
            break

    return {
        "conversation_id": request.conversation_id or "conv_123456",
        "response": response,
        "confidence": 0.9,
        "suggestions": [
            "What's the value of my property?",
            "Best neighborhoods in NYC?",
            "Is it a good time to buy?"
        ],
        "language": request.language,
        "timestamp": "2024-01-25T12:00:00Z"
    }

@app.get("/api/v1/chat/suggestions")
async def get_chat_suggestions():
    """Get chat suggestions."""
    return {
        "suggestions": [
            "What's my property worth?",
            "Find 2-bedroom apartments in Manhattan",
            "Is now a good time to sell?",
            "Which neighborhoods have the best schools?",
            "How do I calculate ROI on rental property?"
        ]
    }

# ==================== ANALYTICS ENDPOINTS ====================
@app.get("/api/v1/analytics/market-trends")
async def get_market_trends():
    """Get market trend analytics."""
    return {
        "date_range": "2024-01-01 to 2024-01-25",
        "average_price": 525000,
        "price_change": "+5.2%",
        "total_listings": 15420,
        "days_on_market": 28,
        "price_per_sqft": 450,
        "trends": {
            "prices": ["increasing", "moderate"],
            "inventory": ["stable", "low"],
            "demand": ["high", "increasing"]
        },
        "top_neighborhoods": [
            {"name": "Upper East Side", "avg_price": 1200000, "change": "+3.2%"},
            {"name": "Williamsburg", "avg_price": 850000, "change": "+7.5%"},
            {"name": "Park Slope", "avg_price": 950000, "change": "+4.1%"}
        ]
    }

@app.get("/api/v1/analytics/property-analytics/{property_id}")
async def get_property_analytics(property_id: str):
    """Get specific property analytics."""
    return {
        "property_id": property_id,
        "views": 1247,
        "saves": 89,
        "inquiries": 23,
        "average_view_time": 45,
        "popular_features": ["doorman", "gym", "parking"],
        "interest_trend": "increasing",
        "similar_properties_viewed": 34,
        "suggested_price_range": {
            "min": 700000,
            "max": 800000
        }
    }

# ==================== UTILITY ENDPOINTS ====================
@app.get("/api/v1/status")
async def get_status():
    """Get system status."""
    return {
        "all_services": "operational",
        "response_time_ms": 45,
        "uptime": "24h 37m",
        "version": "1.0.0",
        "environment": "development",
        "last_deployed": "2024-01-25T10:00:00Z"
    }

@app.get("/api/v1/stats")
async def get_stats():
    """Get API statistics."""
    return {
        "total_requests": 10420,
        "daily_requests": 847,
        "active_users": 234,
        "properties_analyzed": 4521,
        "chats_completed": 1829,
        "average_response_time": "85ms"
    }

# ==================== ERROR HANDLING ====================
@app.exception_handler(404)
async def not_found(request, exc):
    return {"error": "Endpoint not found", "status": 404}

@app.exception_handler(500)
async def internal_error(request, exc):
    return {"error": "Internal server error", "status": 500}

# ==================== MAIN ====================
if __name__ == "__main__":
    print("""
    ╔════════════════════════════════════════════════════════╗
    ║         🚀 GOGIDIX AI SERVICES - ALL-IN-ONE MODE        ║
    ╚════════════════════════════════════════════════════════╝

    Starting server on http://localhost:8000

    Available endpoints:
    ─────────────────────────────────────────────────────────────
    • Health Check:        http://localhost:8000/health
    • API Docs:           http://localhost:8000/docs
    • Property Valuation:  POST /api/v1/property-valuation
    • Chat:               POST /api/v1/chat
    • Market Trends:       GET  /api/v1/analytics/market-trends
    • System Status:       GET  /api/v1/status

    Press Ctrl+C to stop the server
    """)

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info",
        reload=True
    )