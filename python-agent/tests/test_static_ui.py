from pathlib import Path


STATIC_INDEX = Path(__file__).resolve().parents[1] / "app" / "static" / "index.html"


def test_mission_led_coach_studio_main_path() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'id="welcome-panel"' in html
    assert 'id="app-shell"' in html
    assert 'id="mission-launch"' in html
    assert 'id="coach-studio"' in html
    assert 'id="mission-debrief"' in html
    assert 'function renderMissionLaunch()' in html
    assert 'function startMission()' in html
    assert 'function renderCoachStudio()' in html
    assert 'function renderCorrectionState' in html
    assert 'function renderMemorySignal' in html
    assert 'function renderMissionDebrief' in html
    assert 'Coach Studio' in html
    assert 'Memory Signals' in html
    assert 'Mission Debrief' in html
    assert 'data-page="today"' not in html
    assert 'data-page="practice"' not in html
    assert 'data-page="memory"' not in html
    assert 'data-page="progress"' not in html
    assert 'onclick="startCoachSession()"' not in html
    assert 'id="coachUserId"' not in html


def test_daily_plan_composes_mission_model() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'dailyPlan: null' in html
    assert 'mission: null' in html
    assert 'missionStep: ' in html
    assert 'missionSteps: [' in html
    assert 'function ensureDailyPlan()' in html
    assert 'function buildMissionFromPlan(plan)' in html
    assert 'function renderMissionPath' in html
    assert '/api/plans/daily:ensure' in html
    assert 'Turn workplace needs into complete sentences' in html


def test_studio_contains_guided_output_and_correction_modes() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'coachMode: ' in html
    assert 'studioMode: ' in html
    assert 'Guided Output' in html
    assert 'Memory Lock-in' in html
    assert 'function setStudioMode(mode)' in html
    assert 'function submitStudioAnswer()' in html
    assert 'function submitCoachTurn' in html
    assert 'function ensureCoachSession()' in html
    assert 'Write one new sentence' in html


def test_memory_and_review_are_contextual_states() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'priorityMemory: []' in html
    assert 'progressSummary: null' in html
    assert 'coachReview: null' in html
    assert 'function loadPriorityMemory' in html
    assert 'function loadProgressSummary' in html
    assert 'function loadCoachReview' in html
    assert 'function renderMemorySignal' in html
    assert 'function renderMissionDebrief' in html
    assert 'Tomorrow' in html


def test_missing_level_shows_placement_before_today() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'id="placement-panel"' in html
    assert "placementAnswers: []" in html
    assert "function needsPlacement(user)" in html
    assert "function showPlacement()" in html
    assert "function renderPlacementFlow()" in html
    assert "async function submitPlacement()" in html
    assert "/api/placement/assess" in html
    assert "await ensureDailyPlan()" in html
    assert "overallLevel" in html

    enter_body = html.split('async function enterApp()', 1)[1].split('function switchUser()', 1)[0]
    assert "needsPlacement(appState.user)" in enter_body
    assert "showPlacement()" in enter_body
