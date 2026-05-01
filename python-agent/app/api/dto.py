from pydantic import BaseModel


class CoachFeedbackRequest(BaseModel):
    item_content: str
    meaning_zh: str
    user_answer: str | None = None
    result: str  # CORRECT / WRONG
    mode: str
    error_type: str | None = None
    hint_used: bool = False


class CoachFeedbackResponse(BaseModel):
    feedback: str
    encouragement: str


class ErrorExplanationRequest(BaseModel):
    item_content: str
    meaning_zh: str
    user_answer: str | None = None
    mode: str
    error_type: str | None = None


class ErrorExplanationResponse(BaseModel):
    explanation: str
    correct_usage: str
    example: str
