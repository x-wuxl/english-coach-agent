from unittest.mock import patch, MagicMock

from app.api.dto import CoachFeedbackRequest
from app.agents.coach_agent import generate_feedback, FALLBACK_CORRECT, FALLBACK_WRONG


class TestCoachAgent:
    def _make_request(self, result: str = "CORRECT", **kwargs) -> CoachFeedbackRequest:
        defaults = {
            "item_content": "break the ice",
            "meaning_zh": "打破僵局",
            "result": result,
            "mode": "recognition_quiz",
        }
        defaults.update(kwargs)
        return CoachFeedbackRequest(**defaults)

    @patch("app.agents.coach_agent.llm_service")
    def test_correct_answer_returns_feedback(self, mock_llm):
        mock_llm.completion.return_value = "Great job! 'Break the ice' means to start a conversation.\nKeep learning!"

        req = self._make_request(result="CORRECT")
        resp = generate_feedback(req)

        assert "Great job" in resp.feedback
        assert resp.encouragement == "Keep learning!"

    @patch("app.agents.coach_agent.llm_service")
    def test_wrong_answer_returns_feedback(self, mock_llm):
        mock_llm.completion.return_value = "这个短语的意思是'打破僵局'，不是字面意思。\n再试一次！"

        req = self._make_request(result="WRONG", user_answer="打破冰块")
        resp = generate_feedback(req)

        assert "打破僵局" in resp.feedback

    @patch("app.agents.coach_agent.llm_service")
    def test_fallback_on_llm_failure(self, mock_llm):
        mock_llm.completion.return_value = None

        req = self._make_request(result="CORRECT")
        resp = generate_feedback(req)
        assert resp == FALLBACK_CORRECT

        req = self._make_request(result="WRONG")
        resp = generate_feedback(req)
        assert resp == FALLBACK_WRONG
