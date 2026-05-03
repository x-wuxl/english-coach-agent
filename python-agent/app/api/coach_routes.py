from fastapi import APIRouter

from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse, CoachTurnAnalyzeRequest, CoachTurnAnalyzeResponse, FirstCoachingAnalyzeRequest, FirstCoachingAnalyzeResponse
from app.agents.coach_agent import analyze_first_session, analyze_turn, generate_feedback

router = APIRouter(prefix="/api/coach", tags=["coach"])


@router.post("/feedback", response_model=CoachFeedbackResponse)
def coach_feedback(req: CoachFeedbackRequest) -> CoachFeedbackResponse:
    return generate_feedback(req)


@router.post("/turn/analyze", response_model=CoachTurnAnalyzeResponse)
def analyze_coach_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    return analyze_turn(req)


@router.post("/first/analyze", response_model=FirstCoachingAnalyzeResponse)
def analyze_first_coaching(req: FirstCoachingAnalyzeRequest) -> FirstCoachingAnalyzeResponse:
    return analyze_first_session(req)
