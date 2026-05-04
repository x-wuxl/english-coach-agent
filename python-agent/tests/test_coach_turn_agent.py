from unittest.mock import patch

from app.api.dto import CoachTurnAnalyzeRequest
from app.agents.coach_agent import analyze_turn


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_returns_saved_error_pattern(mock_llm):
    mock_llm.structured.return_value = {
        "coach_reply": "What part of the demo feels hardest?",
        "saved_notes": [{
            "type": "ERROR_PATTERN",
            "key": "missing_infinitive_to",
            "label": "need to + verb",
            "description_zh": "need 后面接动词时要加 to。",
            "user_text": "I need prepare the demo.",
            "better_text": "I need to prepare the demo.",
            "severity": "MEDIUM",
            "confidence": 0.9,
        }],
        "expression_gaps": [],
        "fix_response": None,
    }

    resp = analyze_turn(CoachTurnAnalyzeRequest(
        mode="CHAT",
        message="I need prepare the demo.",
        recent_memory=[],
    ))

    assert resp.coach_reply == "What part of the demo feels hardest?"
    assert resp.saved_notes[0].key == "missing_infinitive_to"


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_falls_back_when_structured_result_is_malformed(mock_llm):
    mock_llm.structured.return_value = {"saved_notes": []}

    resp = analyze_turn(CoachTurnAnalyzeRequest(
        mode="CHAT",
        message="I need prepare the demo.",
        recent_memory=[],
    ))

    assert resp.coach_reply == "Tell me more about that."
    assert resp.saved_notes == []


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_fix_mode_returns_fix_response_when_structured_result_is_incomplete(mock_llm):
    mock_llm.structured.return_value = {
        "coach_reply": "Use: I need to prepare the demo.",
        "saved_notes": [],
        "expression_gaps": [],
        "fix_response": None,
    }

    resp = analyze_turn(CoachTurnAnalyzeRequest(
        mode="FIX",
        message="I need prepare the demo.",
        recent_memory=[],
    ))

    assert resp.fix_response is not None
    assert resp.fix_response.better_english
    assert resp.fix_response.try_again_prompt


def test_turn_prompt_includes_learner_context() -> None:
    req = CoachTurnAnalyzeRequest(
        mode="DRILL",
        message="Practice with me.",
        recent_messages=["I need to prepare the demo."],
        learner_context={
            "goal": "GENERAL",
            "todayPlanItems": [{"content": "prepare", "meaningZh": "准备"}],
            "priorityMemory": [{"label": "need to + verb"}],
        },
    )

    from app.agents.coach_agent import _build_turn_prompt

    prompt = _build_turn_prompt(req)

    assert "Learner context" in prompt
    assert "prepare" in prompt
    assert "need to + verb" in prompt


def test_fix_fallback_does_not_repeat_natural_sentence() -> None:
    resp = analyze_turn(CoachTurnAnalyzeRequest(
        mode="FIX",
        message="I need to prepare the demo.",
        recent_memory=[],
    ))

    assert resp.fix_response is not None
    assert resp.fix_response.better_english != "I need to prepare the demo."
    assert "already" in resp.fix_response.meaning_check.lower() or "next" in resp.fix_response.try_again_prompt.lower()
