from pydantic import BaseModel

from app.api.dto import ErrorExplanationRequest, ErrorExplanationResponse
from app.config import settings
from app.services.llm_service import llm_service

SYSTEM_PROMPT = """You are an English teacher explaining mistakes to a Chinese-speaking learner.

Rules:
- Explain in Chinese, with English examples
- Be concise (under 100 words total)
- Clearly state what went wrong and why
- Give the correct usage with a simple example sentence
- Focus on the specific error type if provided
"""

FALLBACK = ErrorExplanationResponse(
    explanation="这道题答错了，建议仔细对比正确答案。",
    correct_usage="请参考正确答案重新理解。",
    example="Practice makes perfect.",
)


class _StructuredExplanation(BaseModel):
    explanation: str
    correct_usage: str
    example: str


def generate_explanation(req: ErrorExplanationRequest) -> ErrorExplanationResponse:
    """Generate error explanation via LLM. Falls back to template if LLM unavailable."""
    user_msg = _build_prompt(req)

    result = llm_service.structured(
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ],
        response_format=_StructuredExplanation,
        model=settings.explanation_model,
        temperature=0.3,
        max_tokens=1024,
    )

    if result is None:
        return FALLBACK

    return ErrorExplanationResponse(
        explanation=result.get("explanation", FALLBACK.explanation),
        correct_usage=result.get("correct_usage", FALLBACK.correct_usage),
        example=result.get("example", FALLBACK.example),
    )


def _build_prompt(req: ErrorExplanationRequest) -> str:
    parts = [
        f"Learning item: {req.item_content}",
        f"Meaning: {req.meaning_zh}",
        f"Mode: {req.mode}",
    ]
    if req.user_answer:
        parts.append(f"User's wrong answer: {req.user_answer}")
    if req.error_type:
        parts.append(f"Error type: {req.error_type}")
    parts.append("\nExplain the mistake and provide the correct usage.")
    return "\n".join(parts)
