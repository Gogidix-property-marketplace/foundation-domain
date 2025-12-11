"""
Text Summarization Service (Port 9042)
AI-powered text summarization service
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
    title="Text Summarization Service",
    description="AI-powered text summarization service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "text-summarization-service",
        "port": 9042,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Text Summarization Service is running",
        "service": "text-summarization-service",
        "port": 9042,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "text-summarization-service",
        "title": "Text Summarization Service",
        "port": 9042,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Text Summarization Service on port 9042")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9042,
        reload=False,
        log_level="info"
    )
