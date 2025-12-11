"""
Image Recognition Service (Port 9025)
AI-powered image recognition service
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
    title="Image Recognition Service",
    description="AI-powered image recognition service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "image-recognition-service",
        "port": 9025,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Image Recognition Service is running",
        "service": "image-recognition-service",
        "port": 9025,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "image-recognition-service",
        "title": "Image Recognition Service",
        "port": 9025,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Image Recognition Service on port 9025")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9025,
        reload=False,
        log_level="info"
    )
