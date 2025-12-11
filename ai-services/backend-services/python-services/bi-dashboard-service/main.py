"""
Bi Dashboard Service (Port 9015)
AI-powered bi dashboard service
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
    title="Bi Dashboard Service",
    description="AI-powered bi dashboard service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "bi-dashboard-service",
        "port": 9015,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Bi Dashboard Service is running",
        "service": "bi-dashboard-service",
        "port": 9015,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "bi-dashboard-service",
        "title": "Bi Dashboard Service",
        "port": 9015,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Bi Dashboard Service on port 9015")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9015,
        reload=False,
        log_level="info"
    )
