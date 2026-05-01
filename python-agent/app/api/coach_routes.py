from fastapi import APIRouter

from app.api.dto import CoachFeedbackRequest, CoachFeedbackResponse
from app.agents.coach_agent import generate_feedback

router = APIRouter(prefix="/api/coach", tags=["coach"])


@router.post("/feedback", response_model=CoachFeedbackResponse)
def coach_feedback(req: CoachFeedbackRequest) -> CoachFeedbackResponse:
    return generate_feedback(req)
