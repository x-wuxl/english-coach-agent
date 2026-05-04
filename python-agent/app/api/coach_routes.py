import logging

from fastapi import APIRouter

from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse, CoachTurnAnalyzeRequest, CoachTurnAnalyzeResponse, FirstCoachingAnalyzeRequest, FirstCoachingAnalyzeResponse
from app.agents.coach_agent import analyze_first_session, analyze_turn, generate_feedback

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/coach", tags=["coach"])


@router.post("/feedback", response_model=CoachFeedbackResponse)
def coach_feedback(req: CoachFeedbackRequest) -> CoachFeedbackResponse:
    logger.info("coach_feedback mode=%s result=%s", req.mode, req.result)
    return generate_feedback(req)


@router.post("/turn/analyze", response_model=CoachTurnAnalyzeResponse)
def analyze_coach_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    logger.info("analyze_coach_turn mode=%s message_len=%d recent_memory=%d", req.mode, len(req.message), len(req.recent_memory))
    resp = analyze_turn(req)
    logger.info("analyze_coach_turn saved_notes=%d expression_gaps=%d", len(resp.saved_notes), len(resp.expression_gaps))
    return resp


@router.post("/first/analyze", response_model=FirstCoachingAnalyzeResponse)
def analyze_first_coaching(req: FirstCoachingAnalyzeRequest) -> FirstCoachingAnalyzeResponse:
    logger.info("analyze_first_coaching daily_minutes=%s samples=%d", req.daily_minutes, len(req.samples))
    resp = analyze_first_session(req)
    logger.info("analyze_first_coaching detected_level_range=%s initial_notes=%d", resp.detected_level_range, len(resp.initial_notes))
    return resp
