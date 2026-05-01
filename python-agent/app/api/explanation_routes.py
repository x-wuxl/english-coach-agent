from fastapi import APIRouter

from app.api.dto import ErrorExplanationRequest, ErrorExplanationResponse
from app.agents.explanation_agent import generate_explanation

router = APIRouter(prefix="/api/explain", tags=["explain"])


@router.post("/error", response_model=ErrorExplanationResponse)
def explain_error(req: ErrorExplanationRequest) -> ErrorExplanationResponse:
    return generate_explanation(req)
