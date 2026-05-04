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
    is_correct: bool = False


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


class SavedNote(BaseModel):
    type: str
    key: str
    label: str
    description_zh: str | None = None
    user_text: str
    better_text: str | None = None
    severity: str = "MEDIUM"
    confidence: float = 0.0


class ExpressionGapNote(BaseModel):
    key: str
    zh_intent: str
    natural_expressions: list[str] = []
    user_attempt: str | None = None
    context: str | None = None
    confidence: float = 0.0


class FixResponse(BaseModel):
    meaning_check: str
    better_english: str
    what_changed: list[str] = []
    memory_update: str | None = None
    try_again_prompt: str | None = None


class CoachTurnAnalyzeRequest(BaseModel):
    mode: str
    message: str
    recent_memory: list[dict] = []
    recent_messages: list[str] = []
    learner_context: dict = {}


class CoachTurnAnalyzeResponse(BaseModel):
    coach_reply: str
    saved_notes: list[SavedNote] = []
    expression_gaps: list[ExpressionGapNote] = []
    fix_response: FixResponse | None = None


class FirstCoachingAnalyzeRequest(BaseModel):
    goal: str
    daily_minutes: int
    samples: list[str]


class FirstCoachingAnalyzeResponse(BaseModel):
    detected_level_range: str
    coach_reply: str
    initial_notes: list[SavedNote] = []
