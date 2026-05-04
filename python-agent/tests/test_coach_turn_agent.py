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
