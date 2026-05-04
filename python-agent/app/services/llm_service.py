import json
import logging
import uuid
from typing import Any

from litellm import completion as litellm_completion

from app.config import settings

logger = logging.getLogger(__name__)


class LLMService:
    """Thin wrapper around LiteLLM with fallback and tracing."""

    def completion(
        self,
        messages: list[dict[str, str]],
        model: str | None = None,
        temperature: float = 0.7,
        max_tokens: int = 1024,
    ) -> str | None:
        """Plain text completion with fallback. Returns None on failure."""
        model = model or settings.llm_default_model
        trace_id = str(uuid.uuid4())[:8]

        result = self._call_completion(model, messages, temperature, max_tokens, trace_id)
        if result is not None:
            return result

        # Fallback to fallback model
        if settings.llm_fallback_model and settings.llm_fallback_model != model:
            logger.info("[%s] Falling back to %s", trace_id, settings.llm_fallback_model)
            result = self._call_completion(settings.llm_fallback_model, messages, temperature, max_tokens, trace_id)
            if result is not None:
                return result

        return None

    def structured(
        self,
        messages: list[dict[str, str]],
        response_format: type,
        model: str | None = None,
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> dict[str, Any] | None:
        """Structured JSON completion with fallback. Returns None on failure."""
        model = model or settings.llm_default_model
        trace_id = str(uuid.uuid4())[:8]

        result = self._call_structured(model, messages, response_format, temperature, max_tokens, trace_id)
        if result is not None:
            return result

        # Fallback to fallback model
        if settings.llm_fallback_model and settings.llm_fallback_model != model:
            logger.info("[%s] Falling back to %s", trace_id, settings.llm_fallback_model)
            result = self._call_structured(settings.llm_fallback_model, messages, response_format, temperature, max_tokens, trace_id)
            if result is not None:
                return result

        return None

    def _build_kwargs(self, model, temperature, max_tokens) -> dict:
        kwargs = {
            "model": model,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "timeout": settings.llm_timeout,
            "num_retries": settings.llm_max_retries,
        }
        if settings.llm_api_key:
            kwargs["api_key"] = settings.llm_api_key
        if settings.llm_api_base:
            kwargs["api_base"] = settings.llm_api_base
        return kwargs

    def _call_completion(self, model, messages, temperature, max_tokens, trace_id) -> str | None:
        try:
            if settings.tracing_enabled:
                logger.info("[%s] LLM call model=%s msgs=%d", trace_id, model, len(messages))

            kwargs = self._build_kwargs(model, temperature, max_tokens)
            response = litellm_completion(messages=messages, **kwargs)

            if settings.tracing_enabled:
                logger.info("[%s] LLM raw response type=%s", trace_id, type(response).__name__)

            # Try multiple ways to extract content
            content = None
            try:
                content = response.choices[0].message.content
            except (AttributeError, IndexError):
                pass

            # If content is empty, try alternative response structures
            if not content and hasattr(response, 'output'):
                # Responses API format
                try:
                    for item in response.output:
                        if hasattr(item, 'content'):
                            for c in item.content:
                                if hasattr(c, 'text'):
                                    content = c.text
                                    break
                except (AttributeError, TypeError):
                    pass

            if not content and hasattr(response, 'output_text'):
                content = response.output_text

            if settings.tracing_enabled:
                logger.info("[%s] LLM response len=%d", trace_id, len(content) if content else 0)

            return content
        except Exception as e:
            logger.warning("[%s] LLM completion failed (model=%s): %s", trace_id, model, e)
            return None

    def _call_structured(self, model, messages, response_format, temperature, max_tokens, trace_id) -> dict | None:
        try:
            if settings.tracing_enabled:
                logger.info("[%s] LLM structured call model=%s", trace_id, model)

            kwargs = self._build_kwargs(model, temperature, max_tokens)
            response = litellm_completion(messages=messages, response_format=response_format, **kwargs)
            content = response.choices[0].message.content

            if settings.tracing_enabled:
                logger.info("[%s] LLM structured response len=%d", trace_id, len(content) if isinstance(content, str) else 0)

            if isinstance(content, str):
                return json.loads(content)
            return content
        except Exception as e:
            logger.warning("[%s] LLM structured completion failed (model=%s): %s", trace_id, model, e)
            return None


llm_service = LLMService()
