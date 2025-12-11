"""
Customer Journey Service (Port 9019)
AI-powered customer journey service
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
    title="Customer Journey Service",
    description="AI-powered customer journey service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "customer-journey-service",
        "port": 9019,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Customer Journey Service is running",
        "service": "customer-journey-service",
        "port": 9019,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "customer-journey-service",
        "title": "Customer Journey Service",
        "port": 9019,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Customer Journey Service on port 9019")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9019,
        reload=False,
        log_level="info"
    )
