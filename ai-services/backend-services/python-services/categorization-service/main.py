"""
Categorization Service (Port 9001)
AI-powered categorization service
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
    title="Categorization Service",
    description="AI-powered categorization service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "categorization-service",
        "port": 9001,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Categorization Service is running",
        "service": "categorization-service",
        "port": 9001,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "categorization-service",
        "title": "Categorization Service",
        "port": 9001,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Categorization Service on port 9001")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9001,
        reload=False,
        log_level="info"
    )
