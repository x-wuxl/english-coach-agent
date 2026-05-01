from unittest.mock import patch

from pydantic import BaseModel

from app.services.correction_service import validate_and_correct


class SampleSchema(BaseModel):
    name: str
    value: int


class TestCorrectionService:
    def test_valid_data_passes_through(self):
        data = {"name": "test", "value": 42}
        result = validate_and_correct([], SampleSchema, data)
        assert result == data

    def test_invalid_data_returns_none_without_llm(self):
        data = {"wrong": "format"}
        result = validate_and_correct([], SampleSchema, data)
        assert result is None

    def test_none_input_returns_none(self):
        result = validate_and_correct([], SampleSchema, None)
        assert result is None

    @patch("app.services.correction_service.llm_service")
    def test_retry_on_parse_failure(self, mock_llm):
        mock_llm.structured.return_value = {"name": "fixed", "value": 1}

        data = {"bad": "data"}
        result = validate_and_correct([{"role": "user", "content": "test"}], SampleSchema, data)
        assert result == {"name": "fixed", "value": 1}
        mock_llm.structured.assert_called_once()
