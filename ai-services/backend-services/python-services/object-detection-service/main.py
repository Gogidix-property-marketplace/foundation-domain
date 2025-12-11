"""
Object Detection Service (Port 9033)
AI-powered object detection service
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
    title="Object Detection Service",
    description="AI-powered object detection service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "object-detection-service",
        "port": 9033,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Object Detection Service is running",
        "service": "object-detection-service",
        "port": 9033,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "object-detection-service",
        "title": "Object Detection Service",
        "port": 9033,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Object Detection Service on port 9033")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9033,
        reload=False,
        log_level="info"
    )
