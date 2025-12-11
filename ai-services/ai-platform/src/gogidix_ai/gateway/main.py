from fastapi import FastAPI
from src.gogidix_ai.core.config import settings

app = FastAPI(title=settings.PROJECT_NAME)

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "AI Gateway"}

@app.get("/")
async def root():
    return {"message": "Gogidix AI Gateway", "version": settings.VERSION}

@app.get("/info")
async def info():
    return {
        "name": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "environment": settings.ENVIRONMENT
    }

