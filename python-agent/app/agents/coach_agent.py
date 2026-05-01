from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse
from app.config import settings
from app.services.llm_service import llm_service

SYSTEM_PROMPT = """You are a friendly English learning coach. Your job is to give brief, encouraging feedback after each learning attempt.

Rules:
- Keep feedback under 80 words
- If correct: acknowledge briefly, add a useful tip about the item
- If wrong: explain the mistake in Chinese, give the correct answer, encourage
- Be warm and coach-like, not robotic
- Mix Chinese and English naturally (中文解释 + English examples)
"""

FALLBACK_CORRECT = CoachFeedbackResponse(
    feedback="Great job! You got it right.",
    encouragement="Keep up the good work!",
)

FALLBACK_WRONG = CoachFeedbackResponse(
    feedback="Not quite, but that's how we learn! Review the answer and try again.",
    encouragement="Don't worry, mistakes help you grow!",
)


def generate_feedback(req: CoachFeedbackRequest) -> CoachFeedbackResponse:
    """Generate coach feedback via LLM. Falls back to template if LLM unavailable."""
    user_msg = _build_prompt(req)
    result = llm_service.completion(
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ],
        model=settings.coach_model,
        temperature=0.7,
        max_tokens=256,
    )

    if result is None:
        return FALLBACK_CORRECT if req.result == "CORRECT" else FALLBACK_WRONG

    # Split feedback and encouragement (LLM returns as one block)
    lines = [l.strip() for l in result.strip().split("\n") if l.strip()]
    feedback = lines[0] if lines else result
    encouragement = lines[1] if len(lines) > 1 else ("Keep going!" if req.result == "CORRECT" else "You can do it!")

    return CoachFeedbackResponse(feedback=feedback, encouragement=encouragement)


def _build_prompt(req: CoachFeedbackRequest) -> str:
    parts = [
        f"Learning item: {req.item_content}",
        f"Meaning: {req.meaning_zh}",
        f"Mode: {req.mode}",
        f"Result: {req.result}",
    ]
    if req.user_answer:
        parts.append(f"User answer: {req.user_answer}")
    if req.error_type:
        parts.append(f"Error type: {req.error_type}")
    if req.hint_used:
        parts.append("(used hint)")
    return "\n".join(parts)
