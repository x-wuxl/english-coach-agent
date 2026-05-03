from pydantic import BaseModel

from app.config import settings
from app.services.llm_service import llm_service


class ReflectionRequest(BaseModel):
    session_type: str
    duration_min: int | None = None
    accuracy: float | None = None
    total_attempts: int = 0
    correct_count: int = 0
    wrong_count: int = 0
    error_types: list[str] = []
    items_studied: list[str] = []


class ReflectionResponse(BaseModel):
    error_summary: str
    pattern_insight: str
    next_session_suggestion: str


SYSTEM_PROMPT = """You are an English learning analyst. Analyze the session data and provide a structured reflection in Chinese.

Rules:
- error_summary: brief summary of what went wrong (under 50 chars)
- pattern_insight: identify learning patterns (under 80 chars)
- next_session_suggestion: actionable advice for next session (under 80 chars)
- Be specific, not generic
- Reference actual error types and items when possible
"""

FALLBACK = ReflectionResponse(
    error_summary="部分题目答错，需要巩固薄弱环节",
    pattern_insight="持续练习有助于巩固记忆",
    next_session_suggestion="建议复习今天答错的内容，保持每日练习节奏",
)


class _StructuredReflection(BaseModel):
    error_summary: str
    pattern_insight: str
    next_session_suggestion: str


def generate_reflection(req: ReflectionRequest) -> ReflectionResponse:
    """Generate session reflection via LLM. Falls back to template if LLM unavailable."""
    user_msg = _build_prompt(req)

    result = llm_service.structured(
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ],
        response_format=_StructuredReflection,
        model=settings.explanation_model,
        temperature=0.4,
        max_tokens=1024,
    )

    if result is None:
        return FALLBACK

    return ReflectionResponse(
        error_summary=result.get("error_summary", FALLBACK.error_summary),
        pattern_insight=result.get("pattern_insight", FALLBACK.pattern_insight),
        next_session_suggestion=result.get("next_session_suggestion", FALLBACK.next_session_suggestion),
    )


def _build_prompt(req: ReflectionRequest) -> str:
    parts = [
        f"Session type: {req.session_type}",
        f"Duration: {req.duration_min or 'unknown'} minutes",
        f"Accuracy: {req.accuracy or 'unknown'}",
        f"Attempts: {req.total_attempts} (correct: {req.correct_count}, wrong: {req.wrong_count})",
    ]
    if req.error_types:
        parts.append(f"Error types: {', '.join(req.error_types)}")
    if req.items_studied:
        parts.append(f"Items studied: {', '.join(req.items_studied[:10])}")
    parts.append("\nAnalyze this session and provide structured reflection.")
    return "\n".join(parts)
