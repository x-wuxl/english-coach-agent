from fastapi.testclient import TestClient
from unittest.mock import patch
import logging

from app.main import app


client = TestClient(app)


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_route(mock_llm):
    mock_llm.structured.return_value = {
        "coach_reply": "Tell me more.",
        "saved_notes": [],
        "expression_gaps": [],
        "fix_response": None,
    }

    resp = client.post("/api/coach/turn/analyze", json={"mode": "CHAT", "message": "Hi", "recent_memory": []})

    assert resp.status_code == 200
    assert resp.json()["coach_reply"] == "Tell me more."


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_route_logs_metadata_without_learner_text(mock_llm, caplog):
    mock_llm.structured.return_value = {
        "coach_reply": "Tell me more.",
        "saved_notes": [],
        "expression_gaps": [],
        "fix_response": None,
    }

    with caplog.at_level(logging.INFO):
        resp = client.post(
            "/api/coach/turn/analyze",
            json={"mode": "CHAT", "message": "Sensitive learner sentence", "recent_memory": []},
        )

    assert resp.status_code == 200
    assert "Sensitive learner sentence" not in caplog.text
