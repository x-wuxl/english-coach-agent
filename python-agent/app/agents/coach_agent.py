from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse
from app.config import settings
from app.services.llm_service import llm_service

SYSTEM_PROMPT = """You are a friendly English learning coach. Your job is to judge if the user's answer is correct and give brief feedback.

Rules:
- First line MUST be exactly "CORRECT" or "WRONG"
- Second line: feedback (under 80 words)
- For RECOGNITION mode: user sees English, types Chinese meaning. Any accurate Chinese translation is acceptable (e.g. "怎样" and "怎么" are equivalent)
- For OUTPUT mode: user sees Chinese, types English sentence. Check grammar and meaning
- If correct: acknowledge briefly, add a useful tip
- If wrong: explain the mistake in Chinese, give the correct answer
- Be warm and coach-like, not robotic
- Mix Chinese and English naturally
"""

FALLBACK_CORRECT = CoachFeedbackResponse(
    feedback="Great job! You got it right.",
    encouragement="Keep up the good work!",
    is_correct=True,
)

FALLBACK_WRONG = CoachFeedbackResponse(
    feedback="Not quite, but that's how we learn! Review the answer and try again.",
    encouragement="Don't worry, mistakes help you grow!",
    is_correct=False,
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

    if result is None or result.strip() == "":
        return FALLBACK_CORRECT if req.result == "CORRECT" else FALLBACK_WRONG

    # Parse LLM response: first line is CORRECT/WRONG, rest is feedback
    lines = [l.strip() for l in result.strip().split("\n") if l.strip()]
    is_correct = False
    feedback_lines = lines

    if lines and lines[0].upper() in ("CORRECT", "WRONG"):
        is_correct = lines[0].upper() == "CORRECT"
        feedback_lines = lines[1:]

    feedback = feedback_lines[0] if feedback_lines else result
    if not feedback.strip():
        feedback = "Answer recorded. Keep practicing!"
    encouragement = feedback_lines[1] if len(feedback_lines) > 1 else ("Keep going!" if is_correct else "You can do it!")

    return CoachFeedbackResponse(feedback=feedback, encouragement=encouragement, is_correct=is_correct)


def _build_prompt(req: CoachFeedbackRequest) -> str:
    parts = []
    if req.mode == "RECOGNITION":
        parts.append(f"English word/phrase: {req.item_content}")
        parts.append(f"Expected Chinese meaning: {req.meaning_zh}")
        parts.append(f"User's answer: {req.user_answer}")
        parts.append("")
        parts.append("Task: Judge if the user's Chinese answer is semantically correct.")
        parts.append("Any accurate Chinese translation is acceptable (e.g. '怎样' = '怎么', '地铁' = '轨道交通').")
    elif req.mode == "OUTPUT":
        parts.append(f"Expected English sentence: {req.item_content}")
        parts.append(f"Chinese meaning: {req.meaning_zh}")
        parts.append(f"User's answer: {req.user_answer}")
        parts.append("")
        parts.append("Task: Judge if the user's English sentence is correct (grammar and meaning).")
    else:
        parts.append(f"Item: {req.item_content}")
        parts.append(f"Meaning: {req.meaning_zh}")
        parts.append(f"User's answer: {req.user_answer}")
        parts.append("")
        parts.append("Task: Judge if the answer is correct.")

    if req.hint_used:
        parts.append("(User used a hint)")
    return "\n".join(parts)
