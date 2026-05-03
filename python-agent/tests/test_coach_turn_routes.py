from fastapi.testclient import TestClient
from unittest.mock import patch

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
