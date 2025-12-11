"""
Sentiment Analysis Service (Port 9043)
AI-powered sentiment analysis service
"""

from fastapi import FastAPI
from typing import Dict, Any
import logging
import os
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="Sentiment Analysis Service",
    description="AI-powered sentiment analysis service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "sentiment-analysis-service",
        "port": 9043,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Sentiment Analysis Service is running",
        "service": "sentiment-analysis-service",
        "port": 9043,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "sentiment-analysis-service",
        "title": "Sentiment Analysis Service",
        "port": 9043,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Sentiment Analysis Service on port 9043")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9043,
        reload=False,
        log_level="info"
    )
