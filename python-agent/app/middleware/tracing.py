import logging
import time
import uuid

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

logger = logging.getLogger(__name__)


class TracingMiddleware(BaseHTTPMiddleware):
    """Adds trace_id to each request and logs timing."""

    async def dispatch(self, request: Request, call_next):
        trace_id = str(uuid.uuid4())[:8]
        request.state.trace_id = trace_id

        start = time.time()
        logger.info("[%s] %s %s", trace_id, request.method, request.url.path)

        response = await call_next(request)

        elapsed_ms = int((time.time() - start) * 1000)
        logger.info("[%s] %d %dms", trace_id, response.status_code, elapsed_ms)
        response.headers["X-Trace-Id"] = trace_id

        return response
