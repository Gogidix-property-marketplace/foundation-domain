"""
Personalization Service (Port 9006)
AI-powered personalization service
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
    title="Personalization Service",
    description="AI-powered personalization service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "personalization-service",
        "port": 9006,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Personalization Service is running",
        "service": "personalization-service",
        "port": 9006,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "personalization-service",
        "title": "Personalization Service",
        "port": 9006,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Personalization Service on port 9006")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9006,
        reload=False,
        log_level="info"
    )
