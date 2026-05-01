from unittest.mock import patch, MagicMock

from app.services.llm_service import LLMService


class TestLLMService:
    def setup_method(self):
        self.service = LLMService()

    @patch("app.services.llm_service.litellm_completion")
    def test_completion_returns_content(self, mock_completion):
        mock_response = MagicMock()
        mock_response.choices = [MagicMock(message=MagicMock(content="Hello!"))]
        mock_completion.return_value = mock_response

        result = self.service.completion([{"role": "user", "content": "Hi"}])
        assert result == "Hello!"
        mock_completion.assert_called_once()

    @patch("app.services.llm_service.litellm_completion")
    def test_completion_returns_none_on_error(self, mock_completion):
        mock_completion.side_effect = Exception("API down")

        result = self.service.completion([{"role": "user", "content": "Hi"}])
        assert result is None

    @patch("app.services.llm_service.litellm_completion")
    def test_structured_returns_dict(self, mock_completion):
        import json
        mock_response = MagicMock()
        mock_response.choices = [MagicMock(message=MagicMock(content=json.dumps({"name": "test"})))]
        mock_completion.return_value = mock_response

        from pydantic import BaseModel
        class TestModel(BaseModel):
            name: str

        result = self.service.structured(
            [{"role": "user", "content": "extract"}],
            response_format=TestModel,
        )
        assert result == {"name": "test"}

    @patch("app.services.llm_service.litellm_completion")
    def test_structured_returns_none_on_error(self, mock_completion):
        mock_completion.side_effect = Exception("API down")

        from pydantic import BaseModel
        class TestModel(BaseModel):
            name: str

        result = self.service.structured(
            [{"role": "user", "content": "extract"}],
            response_format=TestModel,
        )
        assert result is None

    @patch("app.services.llm_service.litellm_completion")
    def test_uses_custom_model(self, mock_completion):
        mock_response = MagicMock()
        mock_response.choices = [MagicMock(message=MagicMock(content="ok"))]
        mock_completion.return_value = mock_response

        self.service.completion([{"role": "user", "content": "Hi"}], model="gpt-4o")
        call_args = mock_completion.call_args
        assert call_args.kwargs["model"] == "gpt-4o"
