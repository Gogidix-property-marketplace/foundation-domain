"""
Matching Algorithm Service (Port 9005)
AI-powered matching algorithm service
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
    title="Matching Algorithm Service",
    description="AI-powered matching algorithm service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "matching-algorithm-service",
        "port": 9005,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Matching Algorithm Service is running",
        "service": "matching-algorithm-service",
        "port": 9005,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "matching-algorithm-service",
        "title": "Matching Algorithm Service",
        "port": 9005,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Matching Algorithm Service on port 9005")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9005,
        reload=False,
        log_level="info"
    )
