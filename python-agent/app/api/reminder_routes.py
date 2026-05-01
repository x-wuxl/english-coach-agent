from datetime import datetime

from fastapi import APIRouter
from pydantic import BaseModel

from app.services.reminder_service import reminder_service

router = APIRouter(prefix="/api/reminders", tags=["reminders"])


class AddReminderRequest(BaseModel):
    user_id: int
    message: str
    scheduled_at: str  # ISO format
    reminder_type: str = "review"


@router.post("")
def add_reminder(req: AddReminderRequest):
    scheduled = datetime.fromisoformat(req.scheduled_at)
    return reminder_service.add_reminder(req.user_id, req.message, scheduled, req.reminder_type)


@router.get("")
def list_reminders(user_id: int | None = None):
    return reminder_service.get_all(user_id)


@router.get("/pending")
def pending_reminders(user_id: int | None = None):
    return reminder_service.get_pending(user_id)
