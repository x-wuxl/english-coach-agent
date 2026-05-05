# Mission-led AI Coach Studio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fully replace the current tab-led English Coach static UI with a production-MVP mission-led AI Coach Studio experience based on the new UI design images in `docs/ui-design/`.

**Architecture:** Rewrite `python-agent/app/static/index.html` as a new native HTML/CSS/JavaScript single-page app and reuse existing Java/Python APIs. Reframe the frontend state around a real daily plan-backed mission, an immersive coach studio, contextual memory signals, and a mission debrief. Do not keep the old visible pages, do not use hard-coded demo data as product data, and do not add a frontend framework unless the project later adopts a Node build pipeline.

**Tech Stack:** Static HTML/CSS/JavaScript, browser `fetch`, `localStorage`, FastAPI static route, Java Spring Boot APIs on port 8080, Python agent on port 8000.

---

## Source References

- Spec: `docs/superpowers/specs/2026-05-05-mission-led-ai-coach-studio-uiux-guide.md`
- Visual references:
  - `docs/ui-design/1.Desktop Mission Launch.png`
  - `docs/ui-design/2.Desktop Immersive Coach Studio.png`
  - `docs/ui-design/3.Real-time Correction State.png`
  - `docs/ui-design/4.Mission Debrief.png`
  - `docs/ui-design/5.Mobile Coach Studio.png`
  - `docs/ui-design/6.Design System Board.png`

## Scope

This plan builds a working MVP frontend redesign. It does not change backend contracts.

In scope:

- Replace visible `Today / Practice / Memory / Progress` navigation with mission-led IA.
- Delete the old visible page structure instead of hiding it behind alternate navigation.
- Add Mission Launch, Coach Studio, correction state, memory signal, and debrief states.
- Reuse existing user entry and placement flow where possible.
- Reuse existing APIs for daily plan, coach turns, priority memory, progress summary, and coach review.
- Use real API data for mission content, memory signals, progress, and review whenever the backend is reachable.
- Provide explicit service-unavailable and empty-data states instead of simulated successful data.
- Update static UI tests to assert the new product contract.
- Verify desktop and mobile layout manually.

Out of scope:

- React/Vue/Svelte migration.
- New Java API endpoints.
- Real authentication.
- Full adaptive curriculum backend.
- Pixel-perfect recreation of generated images.
- Hard-coded fake learning progress, fake memory, or fake coach results as if they were real product data.

## Frontend Framework Decision

Do not introduce a frontend framework in this implementation.

Rationale:

- The repo currently has no Node package, bundler, package lockfile, or frontend build/deploy path.
- The current UI is served directly by FastAPI from `python-agent/app/static/index.html`.
- The immediate goal is an MVP product, not a frontend platform migration.
- Adding React/Vite would create new setup, build, test, and deployment requirements unrelated to the product loop.

Implementation standard:

- Rewrite the static app in clean native HTML/CSS/JavaScript.
- Keep code organized inside `index.html` with clear state, API, render, and event sections.
- If the file becomes too large after the MVP stabilizes, split into static `app.css` and `app.js` in a later refactor. Do not combine that refactor with this product rewrite.

## Real Data Requirements

The UI must use existing backend data paths:

- User profile: `POST /api/users`, `GET /api/users/{userId}` where available.
- Placement: `POST /api/placement/assess`.
- Mission source: `POST /api/plans/daily:ensure`.
- Coach turns: `POST /api/coach/sessions`, `POST /api/coach/sessions/{sessionId}/turns`.
- Memory signals: `GET /api/memory/priority?userId={userId}` and saved notes returned from coach turns.
- Progress: `GET /api/progress/summary?userId={userId}`.
- Debrief/review: `GET /api/coach/review?userId={userId}&startDate={date}&endDate={date}`.

Allowed static fallback:

- Empty-state instructional copy.
- UI labels and example placeholder text inside input fields.
- A deterministic mission title derived from real daily plan data when the backend does not provide a title.

Not allowed:

- Fake completed sessions.
- Fake accuracy.
- Fake saved memories.
- Fake coach replies.
- Fake review summaries shown as if they came from the backend.

## File Structure

Primary implementation file:

- Modify: `python-agent/app/static/index.html`
  - Owns markup, CSS, frontend state, mission/studio rendering, API calls, and interaction states.

Tests:

- Modify: `python-agent/tests/test_static_ui.py`
  - Replace old tab-led assertions with mission-led UI contract assertions.
- Modify: `python-agent/tests/test_ui_route.py`
  - Update served HTML assertion from `ensureDailyPlan` to mission-led app functions.

Docs:

- Keep: `docs/superpowers/specs/2026-05-05-mission-led-ai-coach-studio-uiux-guide.md`
- Keep: `docs/ui-design/*.png`

Do not modify unless a task explicitly requires it:

- `java-core/src/main/java/**`
- `python-agent/app/api/**`
- `python-agent/app/agents/**`

---

### Task 1: Replace Static UI Contract Tests

**Files:**
- Modify: `python-agent/tests/test_static_ui.py`
- Modify: `python-agent/tests/test_ui_route.py`

- [ ] **Step 1: Replace old main-path test with mission-led assertions**

Update `test_coach_cockpit_redesign_main_path` to assert the new IA:

```python
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
```

- [ ] **Step 2: Replace daily plan test with mission composition test**

Add assertions that daily plan data feeds the mission model:

```python
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
```

- [ ] **Step 3: Replace guided practice test with studio exercise mode test**

Add assertions that old practice tools are folded into studio modes:

```python
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
```

- [ ] **Step 4: Replace memory/progress tests with contextual signal/debrief tests**

Add assertions:

```python
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
```

- [ ] **Step 5: Update route test**

In `python-agent/tests/test_ui_route.py`, replace:

```python
assert "function ensureDailyPlan()" in response.text
```

with:

```python
assert "function renderMissionLaunch()" in response.text
assert "Mission-led" in response.text or "Coach Studio" in response.text
```

- [ ] **Step 6: Run tests and confirm they fail**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py tests/test_ui_route.py -q
```

Expected: failures showing missing mission-led functions/markup.

- [ ] **Step 7: Commit test contract**

```powershell
git add python-agent/tests/test_static_ui.py python-agent/tests/test_ui_route.py
git commit -m "test: define mission-led coach studio ui contract"
```

---

### Task 2: Rewrite Static App Shell with Mission-led IA

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Replace `index.html` with a new app skeleton**

Rewrite `python-agent/app/static/index.html` as a fresh document. Do not preserve the old page panels, old navigation, or old markup. The new document must contain only the welcome/placement entry flow and the mission-led app shell.

Use these top-level regions:

```html
<div id="app-shell" class="app-shell hidden">
  <header class="studio-header">
    <div class="brand-mark">
      <span class="brand-dot" aria-hidden="true"></span>
      <div>
        <div class="brand-title">English Coach</div>
        <div class="brand-subtitle">Mission-led AI practice</div>
      </div>
    </div>
    <nav class="studio-nav" aria-label="Coach workspace navigation">
      <button class="nav-link active" data-studio-view="mission" onclick="showStudioView('mission')">Coach Studio</button>
      <button class="nav-link" data-studio-view="signals" onclick="showStudioView('signals')">Memory Signals</button>
      <button class="nav-link" data-studio-view="debrief" onclick="showStudioView('debrief')">Mission Debrief</button>
    </nav>
    <div class="user-menu">
      <span id="current-user-label">-</span>
      <button class="icon-btn" onclick="switchUser()" aria-label="Switch user">Switch</button>
    </div>
  </header>

  <main class="studio-main">
    <section id="mission-launch" class="mission-launch" aria-live="polite"></section>
    <section id="coach-studio" class="coach-studio hidden" aria-live="polite"></section>
    <section id="memory-signals-view" class="signals-view hidden" aria-live="polite"></section>
    <section id="mission-debrief" class="mission-debrief hidden" aria-live="polite"></section>
  </main>
</div>
```

- [ ] **Step 2: Verify old page panels are deleted, not hidden**

Confirm the new file does not contain:

- `data-page-panel="today"`
- `data-page-panel="practice"`
- `data-page-panel="memory"`
- `data-page-panel="progress"`
- `class="main-nav"`
- `function showPage(`
- `id="page-today"`
- `id="page-practice"`
- `id="page-memory"`
- `id="page-progress"`

If behavior from the old app is still needed, reimplement it inside the new state/API/render structure instead of copying old UI sections forward.

- [ ] **Step 3: Add view switching function**

Add:

```javascript
function showStudioView(view) {
    const ids = {
        mission: 'mission-launch',
        studio: 'coach-studio',
        signals: 'memory-signals-view',
        debrief: 'mission-debrief'
    };
    Object.values(ids).forEach(id => document.getElementById(id)?.classList.add('hidden'));
    document.getElementById(ids[view] || ids.mission)?.classList.remove('hidden');
    document.querySelectorAll('.nav-link').forEach(button => {
        const target = button.dataset.studioView;
        button.classList.toggle('active', target === view || (view === 'studio' && target === 'mission'));
    });
}
```

- [ ] **Step 4: Run focused tests**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_ui_route.py -q
```

Expected: route still serves HTML; contract tests may still fail until later tasks.

- [ ] **Step 5: Commit shell rewrite**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: introduce mission-led coach studio shell"
```

---

### Task 3: Add Design System CSS from Visual Board

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Replace CSS tokens**

Use these root tokens near the top of `<style>`:

```css
:root {
  --app-bg: #f5f7f8;
  --surface: #ffffff;
  --surface-raised: #fbfcfd;
  --line: #dce3ea;
  --line-strong: #c7d0dc;
  --text: #162033;
  --muted: #687586;
  --soft: #eef2f6;
  --primary: #2563eb;
  --primary-strong: #1d4ed8;
  --primary-soft: #edf4ff;
  --green: #14845a;
  --green-soft: #ecf8f2;
  --amber: #a95f12;
  --amber-soft: #fff6e8;
  --red: #b42318;
  --red-soft: #fef3f2;
  --radius: 8px;
  --shadow-subtle: 0 1px 2px rgba(15, 23, 42, 0.06);
}
```

- [ ] **Step 2: Add layout CSS for desktop studio**

Add CSS for:

- `.studio-header`
- `.studio-main`
- `.mission-launch`
- `.mission-card`
- `.mission-path`
- `.coach-studio-grid`
- `.mission-rail`
- `.studio-surface`
- `.context-rail`
- `.correction-block`
- `.memory-signal`
- `.debrief-panel`

Use 6-8px radius, light borders, minimal shadows, and no decorative blobs.

- [ ] **Step 3: Add responsive CSS**

At `max-width: 1020px`, collapse studio to two zones.

At `max-width: 760px`, use one-column mobile flow:

```css
@media (max-width: 760px) {
  .studio-header { grid-template-columns: 1fr; gap: 12px; padding: 14px 16px; }
  .studio-nav { overflow-x: auto; justify-content: flex-start; }
  .studio-main { padding: 16px 14px; }
  .coach-studio-grid { grid-template-columns: 1fr; }
  .mission-rail, .context-rail { order: initial; }
  .mission-path { overflow-x: auto; }
  .primary-action, .studio-action { width: 100%; }
}
```

- [ ] **Step 4: Run static tests**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py -q
```

Expected: CSS does not affect route; some contract tests may still fail.

- [ ] **Step 5: Commit visual system**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: add coach studio visual system"
```

---

### Task 4: Add Mission State and Mission Launch

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Extend `appState`**

Add:

```javascript
mission: null,
missionStep: 'launch',
missionSteps: ['Warm-up', 'Build', 'Output', 'Fix', 'Lock-in', 'Debrief'],
studioMode: 'Guided Output',
lastCorrection: null,
memorySignal: null,
coachReview: null,
```

- [ ] **Step 2: Implement `buildMissionFromPlan(plan)`**

Add:

```javascript
function buildMissionFromPlan(plan) {
    const focusItem = findGuidedPracticeItem() || (plan?.newItems || [])[0] || (plan?.reviewItems || [])[0] || null;
    const focus = focusItem?.content || focusItem?.itemCode || 'daily English output';
    const reviewCount = (plan?.reviewItems || []).length;
    const newCount = (plan?.newItems || []).length;
    return {
        title: buildMissionTitle(focusItem, plan),
        rationale: buildMissionRationale(plan),
        duration: appState.user?.dailyMinutes || 10,
        skillFocus: focus,
        pattern: focusItem?.content || focusItem?.itemCode || '',
        steps: appState.missionSteps,
        dueReviews: reviewCount,
        newItems: newCount,
        focusItem
    };
}
```

Also add:

```javascript
function buildMissionTitle(focusItem, plan) {
    if (focusItem?.content) return `Turn ${focusItem.content} into usable English`;
    const reviewCount = (plan?.reviewItems || []).length;
    const newCount = (plan?.newItems || []).length;
    if (reviewCount > 0) return `Recover ${reviewCount} due review item${reviewCount === 1 ? '' : 's'}`;
    if (newCount > 0) return `Build output with ${newCount} new item${newCount === 1 ? '' : 's'}`;
    return 'Start a short adaptive English mission';
}

function buildMissionRationale(plan) {
    const reviewCount = (plan?.reviewItems || []).length;
    const newCount = (plan?.newItems || []).length;
    if (reviewCount && newCount) return `Your plan has ${reviewCount} review item${reviewCount === 1 ? '' : 's'} and ${newCount} new item${newCount === 1 ? '' : 's'} ready.`;
    if (reviewCount) return `Your highest-priority review work is ready for today.`;
    if (newCount) return `Your next new learning items are ready for output practice.`;
    return 'No plan items are available yet. Check the service status or try again.';
}
```

- [ ] **Step 3: Update `enterApp()`**

After user and placement checks, load daily plan, build mission, then render launch:

```javascript
const plan = await ensureDailyPlan();
appState.mission = buildMissionFromPlan(plan);
renderMissionLaunch();
showStudioView('mission');
```

- [ ] **Step 4: Implement `renderMissionLaunch()`**

Render content matching `1.Desktop Mission Launch.png`:

```javascript
function renderMissionLaunch() {
    const mission = appState.mission || buildMissionFromPlan(appState.dailyPlan);
    appState.mission = mission;
    document.getElementById('mission-launch').innerHTML = `
      <div class="mission-card">
        <p class="eyebrow">Today\'s AI Mission</p>
        <h1>${escapeHtml(mission.title)}</h1>
        <p class="mission-rationale">${escapeHtml(mission.rationale)}</p>
        <div class="mission-meta">
          <span>${escapeHtml(mission.duration)} min</span>
          <span>${escapeHtml(mission.skillFocus)}</span>
          <span>${escapeHtml(appState.user?.overallLevel || 'Level pending')}</span>
        </div>
        ${renderMissionPath('launch')}
        <button class="primary-action" onclick="startMission()">Start mission</button>
      </div>
      <aside class="adaptive-signals">
        ${renderMemorySignal(appState.memorySignal)}
      </aside>`;
}
```

If there is no memory signal, `renderMemorySignal(null)` must render an empty state, not a fake saved memory.

- [ ] **Step 5: Implement `startMission()`**

```javascript
async function startMission() {
    appState.missionStep = 'Output';
    appState.studioMode = 'Guided Output';
    await ensureCoachSession();
    renderCoachStudio();
    showStudioView('studio');
}
```

- [ ] **Step 6: Run failing/passing contract tests**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py::test_daily_plan_composes_mission_model -q
```

Expected: pass after implementation.

- [ ] **Step 7: Commit mission launch**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: add adaptive mission launch"
```

---

### Task 5: Implement Immersive Coach Studio

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Implement `renderMissionPath(current)`**

Return a stepper with completed/current/upcoming classes.

- [ ] **Step 2: Implement `setStudioMode(mode)`**

```javascript
function setStudioMode(mode) {
    appState.studioMode = mode;
    renderCoachStudio();
}
```

- [ ] **Step 3: Implement `renderCoachStudio()`**

Render the three-zone layout matching `2.Desktop Immersive Coach Studio.png`:

- left mission rail
- center exercise surface
- right contextual memory signal rail

The center surface should use the current mission's real `pattern` and `focusItem` from the daily plan. The input can include a placeholder, but must not prefill a fake learner answer. Example structure:

```html
<p class="eyebrow">Guided Output</p>
<h1>Use today's focus item in one complete sentence.</h1>
<div class="pattern-token"><!-- real mission pattern --></div>
<textarea id="studio-answer" placeholder="Write one sentence using today's focus item"></textarea>
<button class="studio-action" onclick="submitStudioAnswer()">Check sentence</button>
```

- [ ] **Step 4: Implement `submitStudioAnswer()`**

For this frontend pass, submit the answer through existing coach turn API and render a structured correction when response returns:

```javascript
async function submitStudioAnswer() {
    const answer = document.getElementById('studio-answer')?.value.trim();
    if (!answer || appState.sending) return;
    appState.coachMode = 'FIX';
    document.getElementById('coach-input')?.value = answer;
    await submitCoachTurn(answer);
    appState.lastCorrection = buildCorrectionFromAnswer(answer);
    appState.memorySignal = buildMemorySignalFromCorrection(appState.lastCorrection);
    appState.missionStep = 'Fix';
    appState.studioMode = 'Fix';
    renderCoachStudio();
}
```

If `coach-input` no longer exists after shell replacement, update `submitCoachTurn` to accept message directly without DOM dependency.

- [ ] **Step 5: Run studio mode test**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py::test_studio_contains_guided_output_and_correction_modes -q
```

Expected: pass.

- [ ] **Step 6: Commit studio implementation**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: build immersive coach studio"
```

---

### Task 6: Implement Correction and Memory Signal States

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Implement correction model from real coach response**

Build correction state from the existing coach turn response. Do not synthesize a fake correction when the coach does not return one. Use saved notes, `fix` response fields, or coach reply text where available.

```javascript
function buildCorrectionFromCoachTurn(answer, turnData) {
    const fix = turnData?.fix || turnData?.correction || null;
    const savedNotes = turnData?.savedNotes || [];
    const firstNote = savedNotes[0] || null;
    return {
        original: answer,
        better: fix?.better_english || firstNote?.betterText || '',
        note: fix?.meaning_check || firstNote?.label || turnData?.coachReply || '',
        retryPrompt: fix?.try_again_prompt || 'Write one improved sentence using the coach feedback.',
        savedNotes
    };
}
```

If `better` is empty, render the coach reply as feedback and ask the user to try again; do not invent a corrected sentence.

- [ ] **Step 2: Implement `renderCorrectionState(correction)`**

Render matching `3.Real-time Correction State.png`:

- original sentence
- better sentence
- highlighted inserted `to`
- coach note
- retry prompt
- primary action `Try again`

- [ ] **Step 3: Implement memory signal helpers**

```javascript
function buildMemorySignalFromCorrection(correction) {
    const note = correction?.savedNotes?.[0];
    if (!note && !correction?.better) return null;
    return {
        label: note?.label || note?.key || 'Saved coach feedback',
        sourceText: correction?.original || '',
        betterText: note?.betterText || correction?.better || '',
        reason: note?.memoryType || 'Coach feedback',
        nextUse: 'This signal will be used in a future review mission.'
    };
}
```

- [ ] **Step 4: Implement `renderMemorySignal(signal)`**

Return contextual event markup, not a list/table.

- [ ] **Step 5: Add retry behavior**

`Try again` should reset `missionStep` to `Output`, keep the current memory signal visible, and render the studio answer surface.

- [ ] **Step 6: Run memory/context test**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py::test_memory_and_review_are_contextual_states -q
```

Expected: pass.

- [ ] **Step 7: Commit correction and signal states**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: add correction and memory signal states"
```

---

### Task 7: Implement Mission Debrief

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Add debrief action after lock-in**

After correction retry or lock-in action, show a `View debrief` primary action that calls:

```javascript
async function completeMission() {
    appState.missionStep = 'Debrief';
    await Promise.all([
        loadProgressSummary(),
        loadCoachReview()
    ]);
    renderMissionDebrief();
    showStudioView('debrief');
}
```

- [ ] **Step 2: Update `loadCoachReview()` to store data**

Make `loadCoachReview()` set `appState.coachReview = data.data` before rendering.

- [ ] **Step 3: Implement `renderMissionDebrief()`**

Render matching `4.Mission Debrief.png`, but populate values from `appState.progressSummary`, `appState.coachReview`, `appState.lastCorrection`, and `appState.memorySignal`:

- Mission complete
- Unlocked: saved memory signal and completed exercise count if available
- Still unstable: repeated problems from coach review or weak items from progress summary
- Next mission: `nextPracticeSuggestion`, `nextWeekPlan`, or an empty-state prompt if unavailable
- Actions: `Finish`, `Do bonus drill`

- [ ] **Step 4: Run route and static tests**

Run:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py tests/test_ui_route.py -q
```

Expected: pass.

- [ ] **Step 5: Commit debrief**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: add mission debrief view"
```

---

### Task 8: Mobile Polish and Manual Visual Verification

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Compare mobile CSS against `5.Mobile Coach Studio.png`**

Ensure mobile layout is a single guided flow:

- mission stepper
- current coach prompt
- answer/correction surface
- memory signal drawer
- one primary action

- [ ] **Step 2: Add reduced-motion support**

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 1ms !important;
    transition-duration: 1ms !important;
  }
}
```

- [ ] **Step 3: Start services for manual verification**

Start Java and Python services as usual:

```powershell
docker compose -f docker/docker-compose.yml up -d
cd java-core
mvn spring-boot:run
```

In another terminal:

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m uvicorn app.main:app --reload --port 8000
```

- [ ] **Step 4: Verify desktop manually**

Open `http://localhost:8000/` and verify:

- first entry still works
- placement still appears if level is missing
- Mission Launch appears after setup
- Start mission opens Coach Studio
- Check sentence renders correction
- memory signal appears contextually
- debrief renders
- no old Today/Practice/Memory/Progress nav is visible

- [ ] **Step 5: Verify mobile manually**

Use browser devtools responsive viewport `390x844` and verify:

- no overlapping text
- buttons are touch-sized
- mission stepper does not crowd the screen
- correction text is readable
- memory signal drawer is visible without becoming a dashboard panel

- [ ] **Step 6: Commit polish**

```powershell
git add python-agent/app/static/index.html
git commit -m "feat: polish coach studio responsive layout"
```

---

### Task 9: Final Verification

**Files:**
- Verify only unless fixes are needed.

- [ ] **Step 1: Run Python UI tests**

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_static_ui.py tests/test_ui_route.py -q
```

Expected: all selected tests pass.

- [ ] **Step 2: Run broader Python smoke tests**

```powershell
cd python-agent
C:\Users\wuxl_\.pyenv\pyenv-win\versions\3.11.9\python.exe -m pytest tests/test_health.py tests/test_coach_turn_routes.py tests/test_ui_route.py -q
```

Expected: all selected tests pass.

- [ ] **Step 3: Check git status**

```powershell
git status --short
```

Expected: only intended files changed. Existing unrelated untracked files, such as `tools/smoke-coach-flow.ps1`, should not be touched.

- [ ] **Step 4: Final implementation checklist**

Verify:

- [ ] Mission Launch follows `1.Desktop Mission Launch.png`.
- [ ] Coach Studio follows `2.Desktop Immersive Coach Studio.png`.
- [ ] Correction state follows `3.Real-time Correction State.png`.
- [ ] Debrief follows `4.Mission Debrief.png`.
- [ ] Mobile follows `5.Mobile Coach Studio.png`.
- [ ] Color, spacing, and components are consistent with `6.Design System Board.png`.
- [ ] Old tab-led IA is not visible.
- [ ] Backend APIs are reused, not replaced.
- [ ] Verification results are recorded in final handoff.
