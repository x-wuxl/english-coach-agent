import json
import logging
from typing import Any

from pydantic import BaseModel, ValidationError

from app.services.llm_service import llm_service

logger = logging.getLogger(__name__)


def validate_and_correct(
    messages: list[dict[str, str]],
    response_format: type,
    raw_result: str | dict | None,
    model: str | None = None,
) -> dict[str, Any] | None:
    """Validate LLM structured output. Retry once with simpler prompt on failure."""
    # Step 1: Try to parse raw result
    parsed = _try_parse(response_format, raw_result)
    if parsed is not None:
        return parsed

    # Step 2: Retry with explicit JSON instruction
    logger.info("Correction loop: retrying with explicit JSON instruction")
    retry_messages = messages + [
        {"role": "user", "content": "Please respond with valid JSON only. No extra text."}
    ]

    retry_result = llm_service.structured(
        messages=retry_messages,
        response_format=response_format,
        model=model,
        temperature=0.1,
        max_tokens=512,
    )

    if retry_result is not None:
        parsed = _try_parse(response_format, retry_result)
        if parsed is not None:
            return parsed

    # Step 3: Fallback - return None, caller uses rule-based default
    logger.warning("Correction loop: all attempts failed, falling back to default")
    return None


def _try_parse(response_format: type, data: Any) -> dict[str, Any] | None:
    """Try to parse and validate data against the response_format schema."""
    if data is None:
        return None

    try:
        if isinstance(data, str):
            data = json.loads(data)
        if isinstance(data, dict):
            # Validate required fields exist
            response_format.model_validate(data)
            return data
    except (json.JSONDecodeError, ValidationError, Exception) as e:
        logger.debug("Parse/validation failed: %s", e)

    return None
