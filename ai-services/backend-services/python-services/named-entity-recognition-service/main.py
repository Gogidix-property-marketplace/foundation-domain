"""
Named Entity Recognition Service (Port 9031)
AI-powered named entity recognition service
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
    title="Named Entity Recognition Service",
    description="AI-powered named entity recognition service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "named-entity-recognition-service",
        "port": 9031,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Named Entity Recognition Service is running",
        "service": "named-entity-recognition-service",
        "port": 9031,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "named-entity-recognition-service",
        "title": "Named Entity Recognition Service",
        "port": 9031,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Named Entity Recognition Service on port 9031")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9031,
        reload=False,
        log_level="info"
    )
