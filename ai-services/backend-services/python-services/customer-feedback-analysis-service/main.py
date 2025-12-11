"""
Customer Feedback Analysis Service (Port 9018)
AI-powered customer feedback analysis service
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
    title="Customer Feedback Analysis Service",
    description="AI-powered customer feedback analysis service",
    version="1.0.0"
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "customer-feedback-analysis-service",
        "port": 9018,
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Customer Feedback Analysis Service is running",
        "service": "customer-feedback-analysis-service",
        "port": 9018,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "customer-feedback-analysis-service",
        "title": "Customer Feedback Analysis Service",
        "port": 9018,
        "status": "running",
        "endpoints": ["/", "/health", "/info"]
    }

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Customer Feedback Analysis Service on port 9018")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=9018,
        reload=False,
        log_level="info"
    )
