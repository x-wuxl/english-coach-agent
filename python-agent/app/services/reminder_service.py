import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class Reminder:
    def __init__(self, user_id: int, message: str, scheduled_at: datetime, reminder_type: str = "review"):
        self.user_id = user_id
        self.message = message
        self.scheduled_at = scheduled_at
        self.reminder_type = reminder_type

    def to_dict(self) -> dict:
        return {
            "user_id": self.user_id,
            "message": self.message,
            "scheduled_at": self.scheduled_at.isoformat(),
            "reminder_type": self.reminder_type,
        }


class ReminderService:
    """In-memory reminder store. Replace with Redis/DB in production."""

    def __init__(self):
        self._reminders: list[Reminder] = []

    def add_reminder(self, user_id: int, message: str, scheduled_at: datetime, reminder_type: str = "review"):
        reminder = Reminder(user_id, message, scheduled_at, reminder_type)
        self._reminders.append(reminder)
        logger.info("Added reminder for user %d at %s", user_id, scheduled_at)
        return reminder.to_dict()

    def get_pending(self, user_id: int | None = None) -> list[dict]:
        now = datetime.now()
        pending = [r for r in self._reminders if r.scheduled_at <= now]
        if user_id is not None:
            pending = [r for r in pending if r.user_id == user_id]
        return [r.to_dict() for r in pending]

    def get_all(self, user_id: int | None = None) -> list[dict]:
        reminders = self._reminders
        if user_id is not None:
            reminders = [r for r in reminders if r.user_id == user_id]
        return [r.to_dict() for r in reminders]

    def clear(self, user_id: int):
        self._reminders = [r for r in self._reminders if r.user_id != user_id]


reminder_service = ReminderService()
