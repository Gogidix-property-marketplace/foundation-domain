"""
Text Analysis Service (Port 9041)
AI-powered text analysis service
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
    title="Text Analysis Service",
    description="AI-powered text analysis service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "text-analysis-service",
        "port": 9041,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Text Analysis Service is running",
        "service": "text-analysis-service",
        "port": 9041,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "text-analysis-service",
        "title": "Text Analysis Service",
        "port": 9041,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Text Analysis Service on port 9041")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9041,
        reload=False,
        log_level="info"
    )
