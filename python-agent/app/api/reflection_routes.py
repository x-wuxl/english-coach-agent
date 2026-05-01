from fastapi import APIRouter

from app.agents.reflection_agent import ReflectionRequest, ReflectionResponse, generate_reflection

router = APIRouter(prefix="/api/reflect", tags=["reflect"])


@router.post("/session", response_model=ReflectionResponse)
def reflect_session(req: ReflectionRequest) -> ReflectionResponse:
    return generate_reflection(req)
