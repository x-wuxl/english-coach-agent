from pathlib import Path


STATIC_INDEX = Path(__file__).resolve().parents[1] / "app" / "static" / "index.html"


def test_coach_cockpit_redesign_main_path() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'id="welcome-panel"' in html
    assert 'id="app-shell"' in html
    assert 'data-page="today"' in html
    assert 'data-page="practice"' in html
    assert 'data-page="memory"' in html
    assert 'data-page="progress"' in html
    assert 'data-page="legacy"' not in html
    assert 'onclick="startCoachSession()"' not in html
    assert 'id="coachUserId"' not in html
    assert 'englishCoach.currentUserId' in html
    assert 'function ensureCoachSession()' in html
    assert 'function startGuidedPractice()' in html
    assert 'typing-dots' in html
    assert 'memory-updated' in html

