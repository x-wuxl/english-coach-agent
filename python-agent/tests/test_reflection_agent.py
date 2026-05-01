from unittest.mock import patch

from app.agents.reflection_agent import generate_reflection, ReflectionRequest, FALLBACK


class TestReflectionAgent:
    def _make_request(self, **kwargs) -> ReflectionRequest:
        defaults = {
            "session_type": "DAILY_LEARNING",
            "total_attempts": 10,
            "correct_count": 7,
            "wrong_count": 3,
            "error_types": ["VOCAB_CONFUSION", "COLLOCATION_ERROR"],
        }
        defaults.update(kwargs)
        return ReflectionRequest(**defaults)

    @patch("app.agents.reflection_agent.llm_service")
    def test_returns_structured_reflection(self, mock_llm):
        mock_llm.structured.return_value = {
            "error_summary": "词汇混淆是主要问题",
            "pattern_insight": "搭配类错误集中在动词短语",
            "next_session_suggestion": "建议增加动词短语专项练习",
        }

        req = self._make_request()
        resp = generate_reflection(req)

        assert "词汇" in resp.error_summary
        assert "动词" in resp.pattern_insight

    @patch("app.agents.reflection_agent.llm_service")
    def test_fallback_on_failure(self, mock_llm):
        mock_llm.structured.return_value = None

        req = self._make_request()
        resp = generate_reflection(req)
        assert resp == FALLBACK
