from fastapi import APIRouter, HTTPException

from app.harness.replayer import list_recordings, load_recording

router = APIRouter(prefix="/api/harness", tags=["harness"])


@router.get("/recordings")
def get_recordings() -> list[dict]:
    return list_recordings()


@router.get("/recordings/{filename}")
def get_recording(filename: str) -> dict:
    record = load_recording(filename)
    if record is None:
        raise HTTPException(status_code=404, detail="Recording not found")
    return record
