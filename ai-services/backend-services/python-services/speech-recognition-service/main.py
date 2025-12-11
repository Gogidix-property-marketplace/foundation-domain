"""
Speech Recognition Service (Port 9040)
AI-powered speech recognition service
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
    title="Speech Recognition Service",
    description="AI-powered speech recognition service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "speech-recognition-service",
        "port": 9040,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Speech Recognition Service is running",
        "service": "speech-recognition-service",
        "port": 9040,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "speech-recognition-service",
        "title": "Speech Recognition Service",
        "port": 9040,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Speech Recognition Service on port 9040")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9040,
        reload=False,
        log_level="info"
    )
