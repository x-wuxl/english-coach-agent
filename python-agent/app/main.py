import logging

from fastapi import FastAPI

from app.api.health_routes import router as health_router
from app.api.coach_routes import router as coach_router
from app.api.explanation_routes import router as explanation_router
from app.api.reflection_routes import router as reflection_router
from app.api.harness_routes import router as harness_router
from app.api.reminder_routes import router as reminder_router
from app.api.ui_routes import router as ui_router
from app.middleware.tracing import TracingMiddleware

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

app = FastAPI(title="English Coach Agent")
app.add_middleware(TracingMiddleware)

app.include_router(health_router)
app.include_router(coach_router)
app.include_router(explanation_router)
app.include_router(reflection_router)
app.include_router(harness_router)
app.include_router(reminder_router)
app.include_router(ui_router)
