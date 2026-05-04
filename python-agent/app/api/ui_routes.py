from fastapi import APIRouter
from fastapi.responses import FileResponse
from pathlib import Path

router = APIRouter(tags=["ui"])
STATIC_INDEX = Path(__file__).resolve().parents[1] / "static" / "index.html"


@router.get("/")
def serve_ui():
    return FileResponse(STATIC_INDEX)
