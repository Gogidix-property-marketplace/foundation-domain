"""
Financial Analysis Service (Port 9022)
AI-powered financial analysis service
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
    title="Financial Analysis Service",
    description="AI-powered financial analysis service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "financial-analysis-service",
        "port": 9022,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Financial Analysis Service is running",
        "service": "financial-analysis-service",
        "port": 9022,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "financial-analysis-service",
        "title": "Financial Analysis Service",
        "port": 9022,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Financial Analysis Service on port 9022")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9022,
        reload=False,
        log_level="info"
    )
