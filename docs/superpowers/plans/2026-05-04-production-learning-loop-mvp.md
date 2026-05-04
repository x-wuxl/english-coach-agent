# Production Learning Loop MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current English Coach demo cockpit into a usable learning MVP where a learner can enter with a user code, receive a real daily plan from `learning_item`, complete exercises, update mastery, practice guided sentences, and see memory/progress without manual load steps.

**Architecture:** Keep `java-core` as the source of truth for user profile, content, plans, study sessions, attempts, mastery, memory, and reviews. Keep `python-agent` responsible for LLM-backed coaching and static UI hosting. The work is to connect existing backend learning modules to the static frontend and close the end-to-end product loop; do not replace the architecture or add a frontend framework in this phase.

**Tech Stack:** Ubuntu Linux, Bash, Java 17, Maven, Spring Boot 3, MyBatis-Plus, Flyway, PostgreSQL, Python 3.11, FastAPI, pytest, static HTML/CSS/JavaScript in `python-agent/app/static/index.html`, browser `fetch`, browser `localStorage`, optional Playwright/Chromium headless for UI checks.

---

## Ubuntu And Xshell Execution Notes

This plan is written for an AI agent running on Ubuntu through Xshell or another terminal-only SSH client. Do not require a visible desktop browser.

Use Bash commands and forward-slash paths:

```bash
cd /path/to/english-coach-agent
```

Expected local services:

- Java API: `http://localhost:8080`
- Python/static UI: `http://localhost:8000`
- PostgreSQL from `docker/docker-compose.yml`

Start services in separate terminal sessions, `tmux` panes, or background jobs:

```bash
docker compose -f docker/docker-compose.yml up -d

cd java-core
mvn spring-boot:run

cd ../python-agent
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

If `requirements.txt` does not exist, inspect the existing Python setup files and install the repo's documented dependencies. Do not skip Python tests because of missing environment setup.

## Terminal-Only Acceptance Strategy

Because Xshell cannot rely on a visible browser, use these acceptance layers:

1. API smoke with `curl` and `jq` against real Java/Python services.
2. Database verification with `psql` or `docker exec ... psql` to prove rows are created.
3. Static UI tests with pytest.
4. Headless UI smoke using Playwright/Chromium if available. This can run without a visible browser and can save screenshots to files.

Do not use manual browser inspection as a required gate. Manual browser checks may be optional for a developer with desktop access, but the agent must be able to verify from the terminal.

## Current State Summary

The backend contains real but partially connected learning modules:

- `ContentService` reads `learning_item`.
- `DailyPlanService` can generate plans from `learning_item`.
- `StudySessionService` can record attempts and update `mastery_state`.
- `MasteryStateService`, `MemoryService`, `PlacementService`, and review services exist.

The current website uses only a small subset:

- user code entry;
- coach session and coach turns;
- priority memory top 5;
- manually loaded coach review.

The 80k+ rows in `learning_item` are not used by the normal website flow today because `python-agent/app/static/index.html` does not call `/api/content`, `/api/plans`, `/api/sessions`, `/api/mastery`, `/api/placement`, or `/api/reviews`.

## Non-Goals

- Do not build a React/Vue frontend in this phase.
- Do not implement production auth, passwords, JWT, or account recovery.
- Do not redesign the database schema unless required to close a specific broken loop.
- Do not remove existing backend APIs unless a task explicitly replaces them.
- Do not claim completion based only on isolated unit tests.

## Global Acceptance Criteria

The MVP is accepted only when all of these pass on Ubuntu terminal:

- A learner can enter with a `userCode` and reach Today without manual session IDs.
- If the learner has no plan for today, the app generates one automatically from real `learning_item` rows.
- Today shows actual plan items from `learning_item`, including content, Chinese meaning, difficulty/theme, and examples when available.
- The learner can start a study session, answer at least three items, submit attempts, complete the session, and see mastery/progress change.
- At least one `mastery_state` row is created or updated after the study session.
- Guided Practice no longer routes correct sentences into generic FIX and no longer repeats the same answer as feedback.
- Coach responses receive useful user context: today's plan, recent memory, and current learning focus.
- Progress loads automatically and shows real data from study sessions/mastery, not only coach-turn counts.
- Memory practice is actionable: a memory item can start a drill or guided exercise instead of only pre-filling chat.
- End-to-end smoke scripts pass against real local Java/Python services and PostgreSQL.
- Headless UI smoke or static UI assertions verify the visible flow without a GUI browser.

## File Map

Primary frontend file:

- Modify: `python-agent/app/static/index.html`

Likely Java backend files:

- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/plan/DailyPlanService.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/DailyPlanController.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/dto/DailyPlanItemResponse.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/dto/DailyPlanResponse.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/session/StudySessionService.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/session/dto/StudySessionStartResponse.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/CoachTurnAnalysisRequest.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java`
- Modify or create as needed: `java-core/src/main/java/com/wuxl/englishcoach/api/progress/**`

Likely Python files:

- Modify: `python-agent/app/api/dto.py`
- Modify: `python-agent/app/agents/coach_agent.py`
- Modify: `python-agent/app/api/coach_routes.py`

Tests and smoke:

- Create: `tools/smoke-learning-flow.sh`
- Create: `tools/headless-ui-smoke.js` or equivalent if Playwright is available.
- Modify: `python-agent/tests/test_static_ui.py`
- Modify: Java controller/service tests for plan, session, mastery, coach, progress.

---

## Task 0: Establish Real Verification Baseline

**Purpose:** Prevent repeating the previous issue where controller tests pass but the actual product is unusable.

**Files:**
- Create: `tools/smoke-learning-flow.sh`

- [ ] **Step 1: Create a real-service learning smoke script**

Create a Bash script using `curl` and `jq`. It must call real local services, not mocks.

Required flow:

```text
Python health -> Java health -> load/create user -> ensure daily plan -> extract first learningItemId -> start study session -> submit attempt -> complete session -> verify mastery contains item
```

The script should use this endpoint after Task 1 exists:

```http
POST /api/plans/daily:ensure
```

- [ ] **Step 2: Run the script before implementation**

Expected before implementation: it may fail. Keep the failing result as the baseline.

```bash
chmod +x tools/smoke-learning-flow.sh
JAVA_BASE=http://localhost:8080 PYTHON_BASE=http://localhost:8000 tools/smoke-learning-flow.sh
```

**Acceptance Criteria:**

- `tools/smoke-learning-flow.sh` exists and is executable.
- It checks that a `learning_item` becomes a `mastery_state` through a real study session.
- Later tasks make this script pass without weakening the assertions.

---
## Task 1: Make Daily Plan Get-Or-Create And Frontend-Friendly

**Purpose:** Today must automatically show a real plan from `learning_item`. Users should not know plan API details or recover from duplicate-plan errors.

**Files:**
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/plan/DailyPlanService.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/DailyPlanController.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/dto/DailyPlanItemResponse.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/dto/DailyPlanResponse.java`
- Modify: `java-core/src/test/java/com/wuxl/englishcoach/api/plan/DailyPlanControllerTest.java`

- [ ] **Step 1: Write failing tests for idempotent daily plan loading**

Add tests proving:

- `POST /api/plans/daily:ensure` creates a plan if none exists.
- Calling `POST /api/plans/daily:ensure` twice returns the same active plan and does not throw duplicate errors.
- The returned plan has at least one real `learning_item` when the database has active items.
- Every returned item includes renderable fields: `itemId`, `content`, `meaningZh`, `difficulty`, `theme`, `itemRole`.

- [ ] **Step 2: Add `daily:ensure` controller endpoint**

Recommended endpoint:

```http
POST /api/plans/daily:ensure
Content-Type: application/json

{"userId":1,"planDate":"2026-05-04","planType":"NORMAL"}
```

Expected response: `BaseResponse<DailyPlanResponse>`.

Behavior:

- If a matching active plan exists, return it.
- If no matching plan exists, generate and persist it.
- Do not fail with duplicate-plan errors for the normal frontend path.

- [ ] **Step 3: Enrich `DailyPlanItemResponse`**

Extend the response so the frontend does not need extra content lookups for basic cards:

```java
public record DailyPlanItemResponse(
        Long itemId,
        String itemCode,
        String type,
        String content,
        String meaningZh,
        Integer difficulty,
        String theme,
        java.util.List<java.util.Map<String, String>> examples,
        String itemRole,
        String recommendedMode,
        BigDecimal priorityScore,
        String selectionReason
) {}
```

Update generated-plan and loaded-plan mapping to return the same shape.

- [ ] **Step 4: Stop loading all active items into memory**

Current generation can load all active `learning_item` rows. Replace this with bounded database selection:

- `status = ACTIVE`
- optional theme preference from user sub-goals
- difficulty ordered ascending for beginner-safe defaults
- limit candidate pool, for example 200 rows
- exclude already mastered IDs when practical

This does not need perfect ranking. It must be fast with 80k+ rows.

- [ ] **Step 5: Run targeted tests**

```bash
cd java-core
mvn test -Dtest=DailyPlanControllerTest
```

**Acceptance Criteria:**

- `POST /api/plans/daily:ensure` exists and is idempotent.
- The endpoint returns real `learning_item` data.
- Repeated calls do not fail.
- Plan item response has enough data for a card UI.
- Query behavior is bounded and does not scan the whole content pool in Java.

---

## Task 2: Rewrite Smoke Tests Around Real Product Flows

**Purpose:** Existing tests are too narrow. Smoke tests must prove the app works as a product, and parameter coverage must catch mode/edge-case regressions.

**Files:**
- Create: `tools/smoke-learning-flow.sh`
- Create: `tools/smoke-api-matrix.sh`
- Create: `tools/smoke-db-check.sh`
- Optionally create: `tools/headless-ui-smoke.js`
- Modify: `README.md` only if documenting commands is useful.

- [ ] **Step 1: Create `tools/smoke-learning-flow.sh`**

This is the main happy-path smoke.

Required real-service flow:

```text
GET  python /health
GET  java /api/health
GET  /api/users/by-code/{code} or POST /api/users
POST /api/plans/daily:ensure
POST /api/sessions/start
POST /api/sessions/{sessionId}/attempts
POST /api/sessions/{sessionId}/complete
GET  /api/mastery?userId={id}
GET  /api/memory/priority?userId={id}
```

Required assertions:

- user id exists;
- plan has at least one item;
- selected `learningItemId` is not null;
- study session id exists;
- attempts submission returns success;
- session completion returns success;
- mastery response contains the selected item;
- memory response has an `items` array, even if empty.

- [ ] **Step 2: Create `tools/smoke-api-matrix.sh`**

This script covers parameter variations. It should be fast and deterministic.

Minimum parameter matrix:

User flow:

- existing `userCode` should load, not fail create;
- new `userCode` should create;
- invalid blank user code should return validation error.

Daily plan:

- `planType=NORMAL` should work;
- repeated same date/type should return same or compatible plan, not duplicate error;
- invalid `userId` should return user-not-found response;
- invalid `planDate` should return controlled error, not HTML/500 stack page.

Study attempts:

- `result=CORRECT` updates correct count;
- `result=WRONG` updates wrong count and does not crash if Python explanation fails;
- `mode=recognition_quiz` works;
- `mode=cn_to_en` works;
- `mode=sentence_building` works;
- completed session rejects duplicate completion with a controlled response.

Coach turns:

- `mode=CHAT` returns `coachReply`;
- `mode=FIX` returns meaningful `fixResponse`;
- `mode=FIX` with already natural sentence does not repeat the same sentence as a correction;
- `mode=DRILL` returns a concrete practice prompt;
- blank message returns validation error.

Content/mastery:

- `/api/content/items?page=1&size=5` returns active items;
- `/api/mastery/due-review?userId=...` returns an array;
- `/api/memory/priority?userId=...` returns an array.

- [ ] **Step 3: Create `tools/smoke-db-check.sh`**

This script verifies database effects after the smoke flow. It should support either direct `psql` env vars or Docker Compose PostgreSQL.

Required checks:

```sql
select count(*) from learning_item;
select count(*) from daily_plan_snapshot where user_id = :user_id;
select count(*) from daily_plan_item dpi join daily_plan_snapshot dps on dpi.daily_plan_snapshot_id = dps.id where dps.user_id = :user_id;
select count(*) from study_session where user_id = :user_id and status = 'COMPLETED';
select count(*) from attempt_log where user_id = :user_id;
select count(*) from mastery_state where user_id = :user_id;
```

Acceptance thresholds:

- `learning_item` count > 0, and on the user's DB likely > 80000;
- plan count >= 1;
- plan item count >= 1;
- completed session count >= 1;
- attempt count >= 1;
- mastery count >= 1.

- [ ] **Step 4: Add headless UI smoke if tooling is available**

If Node/Playwright is available, create `tools/headless-ui-smoke.js` that runs Chromium headless.

Required checks:

- Open `http://localhost:8000`.
- Enter a unique user code.
- Continue into app.
- Wait for Today plan section to render.
- Click `Start today`.
- Complete at least one exercise card.
- Confirm progress/memory area updates or success state appears.
- Save screenshots under `tools/smoke-artifacts/`.

Command example:

```bash
node tools/headless-ui-smoke.js
```

If Playwright is unavailable and cannot be installed in the environment, document that the CLI API smoke and static UI test are the required gates for that environment.

- [ ] **Step 5: Make smoke scripts fail loudly**

All smoke scripts must:

- use `set -euo pipefail`;
- print the failing endpoint and response body;
- avoid hiding failures with `|| true` except when intentionally probing a not-found/create branch;
- return non-zero on failure.

**Acceptance Criteria:**

- Smoke testing is rewritten around product flows, not isolated endpoints.
- Parameter matrix covers valid modes, invalid input, repeated operations, and edge cases.
- Database smoke proves records changed in the learning loop.
- Headless UI smoke exists or is explicitly marked unavailable with a reason.

---

## Task 3: Load Today Automatically In Static UI

**Purpose:** The learner should land on Today and immediately see what to do next.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Modify: `python-agent/tests/test_static_ui.py`

- [ ] **Step 1: Add daily plan and study state**

Add to `appState`:

```javascript
dailyPlan: null,
studySession: null,
studyQueue: [],
studyIndex: 0,
studyAttempts: [],
todayLoading: false
```

- [ ] **Step 2: Add `ensureDailyPlan()` API helper**

It should call `/api/plans/daily:ensure`, store `appState.dailyPlan`, build `appState.studyQueue`, and render Today.

- [ ] **Step 3: Call `ensureDailyPlan()` from `enterApp()`**

After user entry, load memory and Today plan automatically:

```javascript
await Promise.all([
    loadPriorityMemory(appState.user.id),
    ensureDailyPlan(),
    loadProgressSummary()
]);
```

- [ ] **Step 4: Render Today plan**

Today should show:

- plan date/status;
- new item count;
- review item count;
- first several plan items;
- `Start today` button;
- loading and error states.

- [ ] **Step 5: Update static UI tests**

Assert that `index.html` contains:

- `function ensureDailyPlan()`;
- `function renderTodayPlan()`;
- `/api/plans/daily:ensure`;
- `Start today`;
- no main Today flow requiring manual `Load`.

Run:

```bash
cd python-agent
source .venv/bin/activate
pytest tests/test_static_ui.py -q
```

**Acceptance Criteria:**

- User entry leads to Today with a loaded plan automatically.
- The UI visibly uses real plan item data.
- No user-facing Today workflow depends on manually entering IDs.

---

## Task 4: Build Minimal Study Runner

**Purpose:** The website must let the learner complete actual learning attempts that update `mastery_state`.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Modify if needed: `java-core/src/main/java/com/wuxl/englishcoach/api/session/dto/StudySessionStartResponse.java`
- Modify: `java-core/src/test/java/com/wuxl/englishcoach/api/session/StudySessionControllerTest.java`
- Modify: `python-agent/tests/test_static_ui.py`

- [ ] **Step 1: Confirm session start response includes `id`**

The frontend needs `data.id` from `/api/sessions/start`. Add it if missing and test it.

- [ ] **Step 2: Add runner functions**

Implement:

```javascript
async function startTodayStudy() {}
function renderStudyRunner() {}
function renderStudyCard(item) {}
function chooseStudyMode(item) {}
function recordStudyAttempt(item, mode, result, responseText, startedAt) {}
async function finishStudySession() {}
```

- [ ] **Step 3: Support three modes**

Minimum modes:

- `recognition_quiz`: buttons `I know it` / `I don't know it`.
- `cn_to_en`: show Chinese, learner types English.
- `sentence_building`: learner writes a sentence using the item.

- [ ] **Step 4: Submit attempts and complete session**

At end of queue:

```text
POST /api/sessions/{sessionId}/attempts
POST /api/sessions/{sessionId}/complete
```

Then refresh Today, memory, and progress.

- [ ] **Step 5: Test mastery update**

Java test must prove that submitting a correct and wrong attempt creates/updates `mastery_state` rows.

Run:

```bash
cd java-core
mvn test -Dtest=StudySessionControllerTest,MasteryControllerTest

cd ../python-agent
source .venv/bin/activate
pytest tests/test_static_ui.py -q
```

**Acceptance Criteria:**

- Browser code can start a study session.
- At least one attempt is submitted to Java.
- Completing a session updates mastery.
- Smoke DB checks show `study_session`, `attempt_log`, and `mastery_state` counts increase.

---

## Task 5: Replace Demo Guided Practice

**Purpose:** Guided Practice must teach a beginner how to build and extend sentences, not send a correct sentence into FIX and repeat it.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Modify: `python-agent/tests/test_static_ui.py`

- [ ] **Step 1: Remove direct FIX routing from `submitGuidedSentence()`**

It must not call `showPage('today')`, `setCoachMode('FIX')`, and `submitCoachTurn()` as the default check behavior.

- [ ] **Step 2: Add local guided practice state**

Use:

```javascript
const guidedPractice = {
    pattern: 'I need to ...',
    verbs: ['prepare', 'join', 'explain'],
    selectedVerb: null,
    step: 1,
    baseSentence: '',
    expandedSentence: '',
    ownSentence: ''
};
```

- [ ] **Step 3: Add pattern checker**

```javascript
function checkNeedToPattern(sentence, verb) {
    const normalized = sentence.trim().replace(/\s+/g, ' ').toLowerCase();
    return normalized.startsWith(`i need to ${verb} `) && normalized.length > `i need to ${verb} `.length;
}
```

- [ ] **Step 4: Make feedback advance steps**

Expected behavior:

- no verb selected -> ask user to choose a verb;
- invalid pattern -> show concise correction;
- valid base sentence -> ask to add time/person/reason;
- valid expanded sentence -> ask for learner's own sentence;
- final step -> show `Send to coach` and optional `Fix this sentence`.

- [ ] **Step 5: Test**

Static test should assert:

- `checkNeedToPattern` exists;
- `submitGuidedSentence()` no longer directly sets FIX mode;
- UI contains next-step wording for guided practice.

**Acceptance Criteria:**

- `I need to prepare the demo.` receives a next-step prompt, not the same FIX response.
- Re-entering the same sentence does not produce identical repeated feedback.
- A beginner can use the flow from the page itself.

---

## Task 6: Give Coach Real Learning Context

**Purpose:** Today Coach should know what the learner is studying and what they struggle with.

**Files:**
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/CoachTurnAnalysisRequest.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java`
- Modify: `python-agent/app/api/dto.py`
- Modify: `python-agent/app/agents/coach_agent.py`
- Modify: `java-core/src/test/java/com/wuxl/englishcoach/api/coach/CoachControllerTest.java`
- Modify: `python-agent/tests/test_coach_turn_agent.py`

- [ ] **Step 1: Extend coach request context**

Recommended Java request:

```java
public record CoachTurnAnalysisRequest(
        String mode,
        String message,
        List<String> recentMessages,
        Map<String, Object> learnerContext
) {}
```

Context should include:

- user goal and level;
- today plan items;
- priority memory;
- current focus;
- recent errors where available.

- [ ] **Step 2: Build context in `CoachSessionService`**

Stop passing `Collections.emptyList()` as the only context. Load available user, plan, and memory data.

- [ ] **Step 3: Update Python DTO and prompt**

Python must accept the context and use it for `CHAT`, `FIX`, and `DRILL`.

- [ ] **Step 4: Improve FIX fallback**

If a sentence is already natural, do not return `Try this: <same sentence>`. Return a meaningful next-step prompt.

Run:

```bash
cd java-core
mvn test -Dtest=CoachControllerTest,PythonAgentClientTest

cd ../python-agent
source .venv/bin/activate
pytest tests/test_coach_turn_agent.py tests/test_coach_turn_routes.py -q
```

**Acceptance Criteria:**

- Java sends non-empty learner context to Python Agent.
- Python accepts the context.
- FIX on a correct sentence does not repeat the same sentence as a correction.
- DRILL produces a concrete practice action tied to memory or plan context when available.

---

## Task 7: Make Progress Real And Automatic

**Purpose:** Progress should show real learning state without manual date loading.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Create or modify if preferred: `java-core/src/main/java/com/wuxl/englishcoach/api/progress/**`
- Add tests for any new Java endpoint.

- [ ] **Step 1: Prefer a progress summary endpoint**

Recommended endpoint:

```http
GET /api/progress/summary?userId={id}
```

Response should include:

- total mastery items;
- due review count;
- completed sessions this week;
- recent accuracy;
- top weak items or error patterns.

- [ ] **Step 2: Implement `loadProgressSummary()`**

Call it after entry and after study completion.

- [ ] **Step 3: Render actionable progress**

Show current stats and what to do next. Manual refresh may remain secondary, but the page must not be empty until clicked.

**Acceptance Criteria:**

- Progress page is useful immediately after app entry.
- Progress changes after completing a study session.
- No date entry is required for basic progress.

---

## Task 8: Make Memory Practice Actionable

**Purpose:** Memory items should start a useful drill, not just prefill a generic chat input.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Modify or create Java memory APIs only if needed.

- [ ] **Step 1: Replace generic `practiceMemory(index)`**

Do not only prefill `Practice: <label>` into Today Coach.

- [ ] **Step 2: Implement memory drill panel**

For `ERROR_PATTERN`:

- show user example;
- ask learner to rewrite;
- reveal better example after attempt.

For `EXPRESSION_GAP`:

- show Chinese intent;
- ask learner to express it in English;
- reveal natural expressions after attempt.

- [ ] **Step 3: Send final attempt to Coach only when useful**

Use `DRILL` mode and include selected memory context.

**Acceptance Criteria:**

- Clicking `Practice` opens a concrete memory exercise.
- The learner can complete the exercise without manually switching pages.
- Coach drill references the selected memory.

---

## Task 9: Add Beginner Placement Path

**Purpose:** Beginners should not be dropped into an open chat box with no idea what to do.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Modify: `java-core/src/test/java/com/wuxl/englishcoach/api/placement/PlacementControllerTest.java` if backend changes are needed.

- [ ] **Step 1: Detect missing level after user entry**

If `user.overallLevel` is missing, show a short placement flow before Today.

- [ ] **Step 2: Add minimal placement UI**

Use 6-8 questions across vocabulary, grammar, reading, and short output. Send simplified answer results to `/api/placement/assess`.

- [ ] **Step 3: Generate first daily plan after placement**

After placement completes, call `ensureDailyPlan()` and move to Today.

**Acceptance Criteria:**

- New user without level sees placement before Today.
- Placement completion updates profile levels.
- First daily plan appears after placement.

---

## Task 10: UI/UX Cleanup After The Loop Works

**Purpose:** Polish only after the real learning loop is connected.

**Files:**
- Modify: `python-agent/app/static/index.html`
- Modify: `python-agent/tests/test_static_ui.py`

- [ ] Remove dead/demo interactions that only prefill text or jump pages.
- [ ] Keep navigation task-focused: Today, Practice, Memory, Progress, Settings/Switch.
- [ ] Add disabled button state, waiting animation, and readable API errors for every network action.
- [ ] Ensure responsive layout works without text overflow.
- [ ] Avoid nested cards and decorative UI that hides the task.

**Acceptance Criteria:**

- The main screen tells the learner what to do next.
- No important flow depends on manual load buttons.
- Buttons and inputs are aligned and responsive.
- Waiting states are visible during coach/API calls.

---

## Task 11: Final Terminal Verification

**Purpose:** Prove the product works from an Ubuntu terminal-only environment.

- [ ] **Step 1: Run Java tests**

```bash
cd java-core
mvn test
```

- [ ] **Step 2: Run Python tests**

```bash
cd python-agent
source .venv/bin/activate
pytest -q
```

- [ ] **Step 3: Start real services**

```bash
docker compose -f docker/docker-compose.yml up -d

cd java-core
mvn spring-boot:run

cd python-agent
source .venv/bin/activate
uvicorn app.main:app --reload --port 8000
```

- [ ] **Step 4: Run smoke scripts**

```bash
JAVA_BASE=http://localhost:8080 PYTHON_BASE=http://localhost:8000 tools/smoke-learning-flow.sh
JAVA_BASE=http://localhost:8080 PYTHON_BASE=http://localhost:8000 tools/smoke-api-matrix.sh
```

Expected:

```text
SMOKE LEARNING FLOW PASS
SMOKE API MATRIX PASS
```

- [ ] **Step 5: Run DB verification**

```bash
tools/smoke-db-check.sh
```

Expected: all required counts are above thresholds.

- [ ] **Step 6: Run headless UI smoke if available**

```bash
node tools/headless-ui-smoke.js
```

Expected: PASS and screenshots saved under `tools/smoke-artifacts/`.

If unavailable, record the missing dependency and make sure static UI tests plus API/database smoke pass.

## Implementation Order

1. Task 0: Real verification baseline.
2. Task 1: Daily plan get-or-create and rich plan item response.
3. Task 2: Rewrite smoke tests and parameter matrix.
4. Task 3: Today automatic plan loading.
5. Task 4: Study runner and mastery update.
6. Task 5: Real Guided Practice.
7. Task 6: Coach learning context.
8. Task 7: Real Progress.
9. Task 8: Actionable Memory practice.
10. Task 9: Beginner Placement.
11. Task 10: UI/UX cleanup.
12. Task 11: Final terminal verification.

Do not start with broad visual redesign. A polished demo is still a demo if `learning_item`, `attempt_log`, and `mastery_state` are not in the user-visible flow.

## Definition Of Done

This plan is done when a learner can use the website for a complete daily study session and the database records prove the loop:

```text
user_profile -> daily_plan_snapshot/daily_plan_item -> study_session -> attempt_log -> mastery_state -> progress/memory
```

The site should be a small but real learning product, not a collection of API demos.
