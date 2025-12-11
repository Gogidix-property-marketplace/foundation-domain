"""
Lead Generation Service (Port 9028)
AI-powered lead generation service
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
    title="Lead Generation Service",
    description="AI-powered lead generation service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "lead-generation-service",
        "port": 9028,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Lead Generation Service is running",
        "service": "lead-generation-service",
        "port": 9028,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "lead-generation-service",
        "title": "Lead Generation Service",
        "port": 9028,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Lead Generation Service on port 9028")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9028,
        reload=False,
        log_level="info"
    )
