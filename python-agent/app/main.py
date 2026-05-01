from fastapi import FastAPI

from app.api.health_routes import router as health_router
from app.api.coach_routes import router as coach_router
from app.api.explanation_routes import router as explanation_router


app = FastAPI(title="English Coach Agent")
app.include_router(health_router)
app.include_router(coach_router)
app.include_router(explanation_router)
