from fastapi import FastAPI
from routers import peak_hours, incidents

app = FastAPI(
    title="G8 ML Service",
    description="Microservice for peak hours and incident predictions",
    version="1.0.0"
)

# Register routers
app.include_router(peak_hours.router)
app.include_router(incidents.router)

@app.get("/health")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
