"""
Image Analysis Service (Port 9024)
AI-powered image analysis service
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
    title="Image Analysis Service",
    description="AI-powered image analysis service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "image-analysis-service",
        "port": 9024,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Image Analysis Service is running",
        "service": "image-analysis-service",
        "port": 9024,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "image-analysis-service",
        "title": "Image Analysis Service",
        "port": 9024,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Image Analysis Service on port 9024")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9024,
        reload=False,
        log_level="info"
    )
