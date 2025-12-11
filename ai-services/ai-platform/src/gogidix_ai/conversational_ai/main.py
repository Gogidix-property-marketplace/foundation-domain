from fastapi import FastAPI

app = FastAPI(title="Conversational AI")

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "Conversational AI"}

@app.get("/")
async def root():
    return {"message": "Conversational AI Service", "status": "active"}
