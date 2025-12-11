"""
Bi Analytics Service (Port 9014)
AI-powered bi analytics service
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
    title="Bi Analytics Service",
    description="AI-powered bi analytics service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "bi-analytics-service",
        "port": 9014,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Bi Analytics Service is running",
        "service": "bi-analytics-service",
        "port": 9014,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "bi-analytics-service",
        "title": "Bi Analytics Service",
        "port": 9014,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Bi Analytics Service on port 9014")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9014,
        reload=False,
        log_level="info"
    )
