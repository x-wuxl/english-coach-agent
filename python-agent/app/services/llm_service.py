import logging
from typing import Any

from litellm import completion as litellm_completion

from app.config import settings

logger = logging.getLogger(__name__)


class LLMService:
    """Thin wrapper around LiteLLM with graceful degradation."""

    def completion(
        self,
        messages: list[dict[str, str]],
        model: str | None = None,
        temperature: float = 0.7,
        max_tokens: int = 1024,
    ) -> str | None:
        """Plain text completion. Returns None on failure."""
        model = model or settings.llm_default_model
        try:
            response = litellm_completion(
                model=model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
                timeout=settings.llm_timeout,
                num_retries=settings.llm_max_retries,
            )
            return response.choices[0].message.content
        except Exception as e:
            logger.warning("LLM completion failed (model=%s): %s", model, e)
            return None

    def structured(
        self,
        messages: list[dict[str, str]],
        response_format: type,
        model: str | None = None,
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> dict[str, Any] | None:
        """Structured JSON completion via Pydantic response_format. Returns None on failure."""
        model = model or settings.llm_default_model
        try:
            response = litellm_completion(
                model=model,
                messages=messages,
                response_format=response_format,
                temperature=temperature,
                max_tokens=max_tokens,
                timeout=settings.llm_timeout,
                num_retries=settings.llm_max_retries,
            )
            content = response.choices[0].message.content
            # LiteLLM returns JSON string when response_format is set
            import json
            if isinstance(content, str):
                return json.loads(content)
            return content
        except Exception as e:
            logger.warning("LLM structured completion failed (model=%s): %s", model, e)
            return None


llm_service = LLMService()
