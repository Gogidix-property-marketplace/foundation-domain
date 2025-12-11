"""
Face Recognition Service (Port 9021)
AI-powered face recognition service
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
    title="Face Recognition Service",
    description="AI-powered face recognition service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "face-recognition-service",
        "port": 9021,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Face Recognition Service is running",
        "service": "face-recognition-service",
        "port": 9021,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "face-recognition-service",
        "title": "Face Recognition Service",
        "port": 9021,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Face Recognition Service on port 9021")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9021,
        reload=False,
        log_level="info"
    )
