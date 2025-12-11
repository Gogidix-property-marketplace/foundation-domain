from fastapi import FastAPI

app = FastAPI(title="Property Intelligence")

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "Property Intelligence"}

@app.get("/")
async def root():
    return {"message": "Property Intelligence Service", "status": "active"}