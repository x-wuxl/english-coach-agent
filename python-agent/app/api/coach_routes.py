from fastapi import APIRouter

from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse, CoachTurnAnalyzeRequest, CoachTurnAnalyzeResponse
from app.agents.coach_agent import analyze_turn, generate_feedback

router = APIRouter(prefix="/api/coach", tags=["coach"])


@router.post("/feedback", response_model=CoachFeedbackResponse)
def coach_feedback(req: CoachFeedbackRequest) -> CoachFeedbackResponse:
    return generate_feedback(req)


@router.post("/turn/analyze", response_model=CoachTurnAnalyzeResponse)
def analyze_coach_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    return analyze_turn(req)
