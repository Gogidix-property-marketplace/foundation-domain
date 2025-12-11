"""
Content Moderation Service (Port 9017)
AI-powered content moderation service
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
    title="Content Moderation Service",
    description="AI-powered content moderation service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "content-moderation-service",
        "port": 9017,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Content Moderation Service is running",
        "service": "content-moderation-service",
        "port": 9017,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "content-moderation-service",
        "title": "Content Moderation Service",
        "port": 9017,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Content Moderation Service on port 9017")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9017,
        reload=False,
        log_level="info"
    )
