from fastapi import APIRouter
from fastapi.responses import FileResponse

router = APIRouter(tags=["ui"])


@router.get("/")
def serve_ui():
    return FileResponse("app/static/index.html")
