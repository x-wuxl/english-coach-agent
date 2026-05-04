import logging

from fastapi import APIRouter

from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse, CoachTurnAnalyzeRequest, CoachTurnAnalyzeResponse, FirstCoachingAnalyzeRequest, FirstCoachingAnalyzeResponse
from app.agents.coach_agent import analyze_first_session, analyze_turn, generate_feedback

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/coach", tags=["coach"])


@router.post("/feedback", response_model=CoachFeedbackResponse)
def coach_feedback(req: CoachFeedbackRequest) -> CoachFeedbackResponse:
    logger.info("coach_feedback request=%s", req.model_dump())
    return generate_feedback(req)


@router.post("/turn/analyze", response_model=CoachTurnAnalyzeResponse)
def analyze_coach_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    logger.info("analyze_coach_turn request=%s", req.model_dump())
    resp = analyze_turn(req)
    logger.info("analyze_coach_turn response=%s", resp.model_dump() if hasattr(resp, "model_dump") else resp)
    return resp


@router.post("/first/analyze", response_model=FirstCoachingAnalyzeResponse)
def analyze_first_coaching(req: FirstCoachingAnalyzeRequest) -> FirstCoachingAnalyzeResponse:
    logger.info("analyze_first_coaching request=%s", req.model_dump())
    resp = analyze_first_session(req)
    logger.info("analyze_first_coaching response=%s", resp.model_dump() if hasattr(resp, "model_dump") else resp)
    return resp
