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


def test_today_plan_loads_automatically() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'dailyPlan: null' in html
    assert 'studySession: null' in html
    assert 'studyQueue: []' in html
    assert 'studyIndex: 0' in html
    assert 'studyAttempts: []' in html
    assert 'todayLoading: false' in html
    assert 'function ensureDailyPlan()' in html
    assert 'function renderTodayPlan()' in html
    assert '/api/plans/daily:ensure' in html
    assert 'Start today' in html
    assert 'onclick="ensureDailyPlan()"' not in html
    assert 'manual plan' not in html.lower()


def test_today_study_runner_submits_real_attempts() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert 'function startTodayStudy()' in html
    assert 'function renderStudyRunner()' in html
    assert 'function renderStudyCard(item)' in html
    assert 'function chooseStudyMode(item)' in html
    assert 'function recordStudyAttempt(item, mode, result, responseText, startedAt)' in html
    assert 'async function finishStudySession()' in html
    assert '/api/sessions/start' in html
    assert '/attempts' in html
    assert '/complete' in html
    assert 'recognition_quiz' in html
    assert 'cn_to_en' in html
    assert 'sentence_building' in html


def test_guided_practice_uses_today_learning_item_before_coach_fix() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert "sourceItem: null" in html
    assert "expression: ''" in html
    assert "function findGuidedPracticeItem()" in html
    assert "function buildGuidedPracticeFromItem(item)" in html
    assert "function checkGuidedPattern(sentence)" in html
    assert "function checkNeedToPattern" not in html
    assert "pattern: 'I need to ...'" not in html
    assert "I need to ${verb} the demo" not in html
    assert "Use today's item" in html
    assert "Add time, person, or reason" in html
    assert "Send to coach" in html

    start_body = html.split('async function startGuidedPractice()', 1)[1].split('function resetGuidedPractice', 1)[0]
    assert "await ensureDailyPlan()" in start_body
    assert "buildGuidedPracticeFromItem" in start_body

    submit_body = html.split('async function submitGuidedSentence()', 1)[1].split('async function sendGuidedSentenceToCoach', 1)[0]
    assert "checkGuidedPattern(sentence)" in submit_body
    assert "setCoachMode('FIX')" not in submit_body
    assert 'await submitCoachTurn()' not in submit_body


def test_progress_loads_automatically_from_summary_endpoint() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert "progressSummary: null" in html
    assert "function loadProgressSummary()" in html
    assert "function renderProgressSummary" in html
    assert "/api/progress/summary" in html
    assert "loadProgressSummary(appState.user.id)" in html
    assert "Total mastery" in html
    assert "Due review" in html


def test_memory_practice_opens_concrete_drill_panel() -> None:
    html = STATIC_INDEX.read_text(encoding="utf-8")

    assert "memoryDrill: null" in html
    assert 'id="memory-drill-panel"' in html
    assert "function startMemoryDrillFromItem(item)" in html
    assert "function renderMemoryDrill" in html
    assert "function submitMemoryDrillAttempt()" in html
    assert "Reveal better example" in html
    assert "Send drill to coach" in html

    practice_body = html.split('function practiceMemory(index)', 1)[1].split('function startMemoryDrillFromItem', 1)[0]
    assert "document.getElementById('coach-input').value" not in practice_body
    assert "Practice:" not in practice_body


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
