from unittest.mock import patch

from app.api.dto import FirstCoachingAnalyzeRequest
from app.agents.coach_agent import analyze_first_session


@patch("app.agents.coach_agent.llm_service")
def test_analyze_first_session_returns_level_and_initial_notes(mock_llm):
    mock_llm.structured.return_value = {
        "detected_level_range": "A2-B1",
        "coach_reply": "You can handle simple work topics, and we will focus on verb patterns.",
        "initial_notes": [{
            "type": "ERROR_PATTERN",
            "key": "missing_infinitive_to",
            "label": "want to + verb",
            "description_zh": "Use to after want before a verb.",
            "user_text": "I want improve my English.",
            "better_text": "I want to improve my English.",
            "severity": "MEDIUM",
            "confidence": 0.9,
        }],
    }

    resp = analyze_first_session(FirstCoachingAnalyzeRequest(
        goal="GENERAL",
        daily_minutes=10,
        samples=["I want improve my English because my work need it."],
    ))

    assert resp.detected_level_range == "A2-B1"
    assert resp.initial_notes[0].key == "missing_infinitive_to"
