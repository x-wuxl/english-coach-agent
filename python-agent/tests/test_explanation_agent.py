from unittest.mock import patch, MagicMock

from app.api.dto import ErrorExplanationRequest
from app.agents.explanation_agent import generate_explanation, FALLBACK


class TestExplanationAgent:
    def _make_request(self, **kwargs) -> ErrorExplanationRequest:
        defaults = {
            "item_content": "break the ice",
            "meaning_zh": "打破僵局",
            "mode": "cn_to_en",
            "user_answer": "break the ice cube",
        }
        defaults.update(kwargs)
        return ErrorExplanationRequest(**defaults)

    @patch("app.agents.explanation_agent.llm_service")
    def test_returns_structured_explanation(self, mock_llm):
        mock_llm.structured.return_value = {
            "explanation": "'break the ice' 是固定短语，不能随意改词。",
            "correct_usage": "break the ice = 打破僵局",
            "example": "Let me break the ice by introducing myself.",
        }

        req = self._make_request()
        resp = generate_explanation(req)

        assert "break the ice" in resp.correct_usage
        assert "example" in resp.example.lower() or "break the ice" in resp.example

    @patch("app.agents.explanation_agent.llm_service")
    def test_fallback_on_llm_failure(self, mock_llm):
        mock_llm.structured.return_value = None

        req = self._make_request()
        resp = generate_explanation(req)
        assert resp == FALLBACK

    @patch("app.agents.explanation_agent.llm_service")
    def test_partial_result_uses_defaults(self, mock_llm):
        mock_llm.structured.return_value = {"explanation": "wrong", "correct_usage": "", "example": ""}

        req = self._make_request()
        resp = generate_explanation(req)
        assert resp.explanation == "wrong"
