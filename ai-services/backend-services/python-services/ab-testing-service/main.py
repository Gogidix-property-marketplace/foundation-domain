"""
Ab Testing Service (Port 9000)
AI-powered ab testing service
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
    title="Ab Testing Service",
    description="AI-powered ab testing service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "ab-testing-service",
        "port": 9000,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Ab Testing Service is running",
        "service": "ab-testing-service",
        "port": 9000,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "ab-testing-service",
        "title": "Ab Testing Service",
        "port": 9000,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Ab Testing Service on port 9000")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9000,
        reload=False,
        log_level="info"
    )
