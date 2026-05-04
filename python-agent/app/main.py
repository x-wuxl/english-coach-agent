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


class RawBodyLoggingMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope.get("type") != "http" or scope.get("path") != "/api/coach/turn/analyze":
            await self.app(scope, receive, send)
            return

        messages = []
        chunks = []
        while True:
            message = await receive()
            messages.append(message)
            if message.get("type") == "http.request":
                chunks.append(message.get("body", b""))
                if not message.get("more_body", False):
                    break
            else:
                break

        headers = {k.decode("latin1"): v.decode("latin1") for k, v in scope.get("headers", [])}
        body = b"".join(chunks)
        logger.info(
            "Raw incoming body path=%s content_type=%s content_length=%s body_len=%d body=%s",
            scope.get("path"),
            headers.get("content-type"),
            headers.get("content-length"),
            len(body),
            body[:4000].decode("utf-8", errors="replace"),
        )

        replay = iter(messages)

        async def replay_receive():
            try:
                return next(replay)
            except StopIteration:
                return {"type": "http.request", "body": b"", "more_body": False}

        await self.app(scope, replay_receive, send)


app = FastAPI(title="English Coach Agent")
app.add_middleware(TracingMiddleware)
app.add_middleware(RawBodyLoggingMiddleware)


@app.exception_handler(RequestValidationError)
async def request_validation_exception_handler(request: Request, exc: RequestValidationError):
    try:
        body = (await request.body()).decode("utf-8", errors="replace")
    except Exception as body_error:
        body = f"<failed to read body: {body_error}>"

    logger.warning(
        "Request validation failed path=%s method=%s content_type=%s errors=%s body=%s",
        request.url.path,
        request.method,
        request.headers.get("content-type"),
        exc.errors(),
        body[:4000],
    )
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    try:
        body = (await request.body()).decode("utf-8", errors="replace")
    except Exception as body_error:
        body = f"<failed to read body: {body_error}>"

    logger.exception(
        "Unhandled request error path=%s method=%s content_type=%s body=%s",
        request.url.path,
        request.method,
        request.headers.get("content-type"),
        body[:4000],
    )
    return JSONResponse(status_code=500, content={"detail": "Internal Server Error"})


app.include_router(health_router)
app.include_router(coach_router)
app.include_router(explanation_router)
app.include_router(reflection_router)
app.include_router(harness_router)
app.include_router(reminder_router)
app.include_router(ui_router)
