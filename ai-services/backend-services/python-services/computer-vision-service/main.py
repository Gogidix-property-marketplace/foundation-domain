"""
Computer Vision Service (Port 9002)
AI-powered computer vision service
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
    title="Computer Vision Service",
    description="AI-powered computer vision service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "computer-vision-service",
        "port": 9002,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Computer Vision Service is running",
        "service": "computer-vision-service",
        "port": 9002,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "computer-vision-service",
        "title": "Computer Vision Service",
        "port": 9002,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Computer Vision Service on port 9002")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9002,
        reload=False,
        log_level="info"
    )
