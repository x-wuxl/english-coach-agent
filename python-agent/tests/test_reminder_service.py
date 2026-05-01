from datetime import datetime, timedelta

from app.services.reminder_service import ReminderService


class TestReminderService:
    def setup_method(self):
        self.service = ReminderService()

    def test_add_and_list(self):
        now = datetime.now()
        self.service.add_reminder(1, "Review vocab", now + timedelta(hours=1))
        self.service.add_reminder(1, "Review grammar", now + timedelta(hours=2))

        all_reminders = self.service.get_all(1)
        assert len(all_reminders) == 2
        assert all_reminders[0]["message"] == "Review vocab"

    def test_pending_only_returns_due(self):
        now = datetime.now()
        self.service.add_reminder(1, "Past due", now - timedelta(hours=1))
        self.service.add_reminder(1, "Future", now + timedelta(hours=1))

        pending = self.service.get_pending(1)
        assert len(pending) == 1
        assert pending[0]["message"] == "Past due"

    def test_clear_user_reminders(self):
        self.service.add_reminder(1, "test", datetime.now())
        self.service.add_reminder(2, "other", datetime.now())

        self.service.clear(1)
        assert len(self.service.get_all(1)) == 0
        assert len(self.service.get_all(2)) == 1

    def test_filter_by_user(self):
        self.service.add_reminder(1, "user1", datetime.now())
        self.service.add_reminder(2, "user2", datetime.now())

        assert len(self.service.get_all(1)) == 1
        assert len(self.service.get_all(2)) == 1
        assert len(self.service.get_all()) == 2
