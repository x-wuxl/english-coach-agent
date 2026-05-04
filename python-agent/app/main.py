import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.health_routes import router as health_router
from app.api.coach_routes import router as coach_router
from app.api.explanation_routes import router as explanation_router
from app.api.reflection_routes import router as reflection_router
from app.api.harness_routes import router as harness_router
from app.api.reminder_routes import router as reminder_router
from app.api.ui_routes import router as ui_router
from app.middleware.tracing import TracingMiddleware

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)


app = FastAPI(title="English Coach Agent")
app.add_middleware(TracingMiddleware)


@app.exception_handler(RequestValidationError)
async def request_validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.warning(
        "Request validation failed path=%s method=%s content_type=%s content_length=%s errors=%s",
        request.url.path,
        request.method,
        request.headers.get("content-type"),
        request.headers.get("content-length"),
        exc.errors(),
    )
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception(
        "Unhandled request error path=%s method=%s content_type=%s content_length=%s",
        request.url.path,
        request.method,
        request.headers.get("content-type"),
        request.headers.get("content-length"),
    )
    return JSONResponse(status_code=500, content={"detail": "Internal Server Error"})


app.include_router(health_router)
app.include_router(coach_router)
app.include_router(explanation_router)
app.include_router(reflection_router)
app.include_router(harness_router)
app.include_router(reminder_router)
app.include_router(ui_router)
