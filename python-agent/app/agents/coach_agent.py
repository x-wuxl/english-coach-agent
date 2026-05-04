from pydantic import ValidationError

from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse, CoachTurnAnalyzeRequest, CoachTurnAnalyzeResponse, FirstCoachingAnalyzeRequest, FirstCoachingAnalyzeResponse, FixResponse
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

TURN_ANALYSIS_SYSTEM_PROMPT = """You are an English coach for a conversational learning cockpit.

Return structured JSON only. Keep memory suggestions lightweight and learner-owned:
- coach_reply: one concise coaching response in English.
- saved_notes: grammar or usage issues worth remembering.
- expression_gaps: Chinese intent the learner could not express naturally.
- fix_response: only when mode is FIX.

Do not schedule drills or decide long-term memory priority. The Java service owns persistence and scheduling.
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
        max_tokens=32000,
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


def analyze_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    result = llm_service.structured(
        messages=[
            {"role": "system", "content": TURN_ANALYSIS_SYSTEM_PROMPT},
            {"role": "user", "content": _build_turn_prompt(req)},
        ],
        response_format=CoachTurnAnalyzeResponse,
        model=settings.coach_model,
        temperature=0.4,
        max_tokens=4000,
    )
    if result is None:
        return _fallback_turn_response(req)
    if isinstance(result, CoachTurnAnalyzeResponse):
        return _ensure_fix_response(req, result)
    try:
        return _ensure_fix_response(req, CoachTurnAnalyzeResponse(**result))
    except (TypeError, ValidationError):
        return _fallback_turn_response(req)


def _ensure_fix_response(req: CoachTurnAnalyzeRequest, resp: CoachTurnAnalyzeResponse) -> CoachTurnAnalyzeResponse:
    if req.mode.upper() != "FIX" or resp.fix_response is not None:
        return resp
    fallback = _fallback_fix_response(req.message)
    return resp.model_copy(update={
        "coach_reply": resp.coach_reply or fallback.coach_reply,
        "fix_response": fallback.fix_response,
    })


def _fallback_turn_response(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    if req.mode.upper() == "FIX":
        return _fallback_fix_response(req.message)
    return CoachTurnAnalyzeResponse(coach_reply="Tell me more about that.")


def _fallback_fix_response(message: str) -> CoachTurnAnalyzeResponse:
    better = _basic_fix_sentence(message)
    if _same_sentence(message, better):
        next_step = _extend_natural_sentence(message)
        return CoachTurnAnalyzeResponse(
            coach_reply="This sentence is already natural. Try adding one useful detail next.",
            fix_response=FixResponse(
                meaning_check="This sentence is already natural and clear.",
                better_english=next_step,
                what_changed=["No grammar correction needed; this is an expansion for more useful output."],
                try_again_prompt="Next, add time, reason, or a person to make it more specific.",
            ),
        )
    return CoachTurnAnalyzeResponse(
        coach_reply=f"Try this: {better}",
        fix_response=FixResponse(
            meaning_check="I understood your sentence and made it more natural.",
            better_english=better,
            what_changed=["Checked the sentence structure and adjusted the wording."],
            try_again_prompt="Try one more sentence with the corrected pattern.",
        ),
    )


def _basic_fix_sentence(message: str) -> str:
    text = (message or "").strip()
    if not text:
        return "Please write one complete English sentence."
    fixes = [
        ("I need prepare", "I need to prepare"),
        ("i need prepare", "I need to prepare"),
        ("I want improve", "I want to improve"),
        ("i want improve", "I want to improve"),
        ("I discussed about", "I discussed"),
        ("i discussed about", "I discussed"),
    ]
    for old, new in fixes:
        if old in text:
            text = text.replace(old, new)
    return text[0].upper() + text[1:]



def _same_sentence(left: str, right: str) -> bool:
    return " ".join((left or "").strip().lower().split()) == " ".join((right or "").strip().lower().split())


def _extend_natural_sentence(message: str) -> str:
    base = (message or "").strip().rstrip(".!?")
    if not base:
        return "Please write one complete English sentence."
    return f"{base} before Friday."

def analyze_first_session(req: FirstCoachingAnalyzeRequest) -> FirstCoachingAnalyzeResponse:
    result = llm_service.structured(
        messages=[
            {"role": "system", "content": TURN_ANALYSIS_SYSTEM_PROMPT},
            {"role": "user", "content": _build_first_session_prompt(req)},
        ],
        response_format=FirstCoachingAnalyzeResponse,
        model=settings.coach_model,
        temperature=0.4,
        max_tokens=4000,
    )
    if result is None:
        return FirstCoachingAnalyzeResponse(
            detected_level_range="A2-B1",
            coach_reply="We will start with clear work and daily English, then tighten repeated patterns.",
            initial_notes=[],
        )
    if isinstance(result, FirstCoachingAnalyzeResponse):
        return result
    try:
        return FirstCoachingAnalyzeResponse(**result)
    except (TypeError, ValidationError):
        return FirstCoachingAnalyzeResponse(
            detected_level_range="A2-B1",
            coach_reply="We will start with clear work and daily English, then tighten repeated patterns.",
            initial_notes=[],
        )


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


def _build_turn_prompt(req: CoachTurnAnalyzeRequest) -> str:
    parts = [
        f"Mode: {req.mode}",
        f"Learner message: {req.message}",
        f"Recent messages: {req.recent_messages}",
        f"Recent memory: {req.recent_memory}",
        f"Learner context: {req.learner_context}",
    ]
    return "\n".join(parts)


def _build_first_session_prompt(req: FirstCoachingAnalyzeRequest) -> str:
    return "\n".join([
        f"Goal: {req.goal}",
        f"Daily minutes: {req.daily_minutes}",
        "Samples:",
        *req.samples,
    ])
