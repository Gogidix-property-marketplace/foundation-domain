from fastapi import FastAPI

app = FastAPI(title="Analytics")

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "Analytics"}

@app.get("/")
async def root():
    return {"message": "Analytics Service", "status": "active"}
