# Coach Cockpit UI/UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the static English Coach cockpit so learners can start quickly, use Today as the main workspace, access beginner-friendly guided practice, and receive clear interaction feedback without manual user/session operations.

**Architecture:** Keep the current static HTML application in `python-agent/app/static/index.html` and reuse existing Java/Python APIs. Introduce a small frontend state model backed by `localStorage`, auto-create coach sessions on first message, and reshape the UI into `Today`, `Practice`, `Memory`, and `Progress` views. Do not add real authentication or rewrite backend services in this phase.

**Tech Stack:** Static HTML/CSS/JavaScript, browser `fetch`, browser `localStorage`, Java Spring Boot APIs on port 8080, FastAPI static hosting on port 8000.

---

## Source Spec

- `docs/superpowers/specs/2026-05-04-coach-cockpit-ui-ux-redesign.md`

## File Structure

Primary file:

- Modify: `python-agent/app/static/index.html`
  - Owns the static UI, visual system, navigation, frontend state, API calls, Today/Practice/Memory/Progress rendering, and interaction states.

Possible test/verification files:

- Existing Python tests remain unchanged unless static route behavior breaks.
- No automated browser harness currently exists. Use manual browser verification for UI behavior in this phase.

Do not modify in this plan unless a task explicitly calls for it:

- `java-core/src/main/java/**`
- `python-agent/app/api/**`
- `python-agent/app/agents/**`

---

### Task 1: Frontend App State and User Entry

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Add state constants and current-user helpers**

Add near the top of the `<script>` block, replacing scattered user/session globals where appropriate:

```javascript
const STORAGE_KEYS = {
    userId: 'englishCoach.currentUserId',
    userCode: 'englishCoach.currentUserCode',
    goal: 'englishCoach.currentUserGoal',
    dailyMinutes: 'englishCoach.currentDailyMinutes'
};

const appState = {
    user: null,
    coachSessionId: null,
    coachMode: 'CHAT',
    coachMessages: [],
    priorityMemory: [],
    sending: false,
    pendingMessageId: null
};

function loadStoredUser() {
    const id = localStorage.getItem(STORAGE_KEYS.userId);
    const userCode = localStorage.getItem(STORAGE_KEYS.userCode);
    if (!id || !userCode) return null;
    return {
        id: Number(id),
        userCode,
        goal: localStorage.getItem(STORAGE_KEYS.goal) || 'GENERAL',
        dailyMinutes: Number(localStorage.getItem(STORAGE_KEYS.dailyMinutes) || 10)
    };
}

function storeCurrentUser(user) {
    localStorage.setItem(STORAGE_KEYS.userId, String(user.id));
    localStorage.setItem(STORAGE_KEYS.userCode, user.userCode);
    localStorage.setItem(STORAGE_KEYS.goal, user.goal || 'GENERAL');
    localStorage.setItem(STORAGE_KEYS.dailyMinutes, String(user.dailyMinutes || 10));
}

function clearCurrentUser() {
    Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key));
    appState.user = null;
    appState.coachSessionId = null;
}
```

- [ ] **Step 2: Replace the visible manual user controls with a welcome/auth-lite panel**

In the body, add a welcome panel that is shown when `appState.user` is null:

```html
<section id="welcome-panel" class="welcome-panel hidden">
  <div class="welcome-card">
    <h1>English Coach</h1>
    <p>Start with a lightweight learning profile.</p>
    <label>User code</label>
    <input id="entryUserCode" type="text" placeholder="e.g. wuxl" autocomplete="off" />
    <div class="entry-grid">
      <div>
        <label>Goal</label>
        <select id="entryGoal">
          <option value="GENERAL">General</option>
          <option value="WORKPLACE">Workplace</option>
        </select>
      </div>
      <div>
        <label>Daily minutes</label>
        <input id="entryDailyMinutes" type="number" min="5" max="180" value="10" />
      </div>
    </div>
    <div id="entry-msg"></div>
    <button class="btn btn-primary" id="entryContinueBtn" onclick="continueWithUser()">Continue</button>
  </div>
</section>
```

- [ ] **Step 3: Implement `continueWithUser()`**

Use existing `POST /api/users`:

```javascript
async function continueWithUser() {
    const userCode = document.getElementById('entryUserCode').value.trim();
    const goal = document.getElementById('entryGoal').value;
    const dailyMinutes = Number(document.getElementById('entryDailyMinutes').value || 10);
    if (!userCode) {
        showMsg('entry-msg', 'Enter a user code to continue.', 'error');
        return;
    }

    setButtonBusy('entryContinueBtn', true, 'Continuing...');
    try {
        const data = await apiPost(`${JAVA_BASE}/api/users`, {
            userCode,
            goal,
            dailyMinutes
        });
        if (data.code !== 0) {
            showMsg('entry-msg', 'Could not create this user code. Use a new code or switch to a saved local user.', 'error');
            return;
        }
        appState.user = data.data;
        storeCurrentUser(data.data);
        await enterApp();
    } finally {
        setButtonBusy('entryContinueBtn', false, 'Continue');
    }
}
```

- [ ] **Step 4: Implement app initialization**

Call this after function declarations or on `DOMContentLoaded`:

```javascript
document.addEventListener('DOMContentLoaded', initApp);

async function initApp() {
    appState.user = loadStoredUser();
    if (!appState.user) {
        showWelcome();
        return;
    }
    await enterApp();
}

function showWelcome() {
    document.getElementById('welcome-panel').classList.remove('hidden');
    document.getElementById('app-shell').classList.add('hidden');
}

async function enterApp() {
    document.getElementById('welcome-panel').classList.add('hidden');
    document.getElementById('app-shell').classList.remove('hidden');
    renderUserSummary();
    showPage('today');
    await loadPriorityMemory(appState.user.id);
    setDefaultReviewDates();
}
```

- [ ] **Step 5: Add switch-user behavior**

```javascript
function switchUser() {
    clearCurrentUser();
    appState.coachMessages = [];
    appState.priorityMemory = [];
    renderCoachStream();
    showWelcome();
}
```

- [ ] **Step 6: Manually verify first-run flow**

Run:

```powershell
cd python-agent
uvicorn app.main:app --reload --port 8000
```

With java-core running, open `http://localhost:8000/`.

Expected:

- No manual `User ID` / `Load` controls are needed on the main path.
- Welcome panel appears with no stored user.
- Creating a user stores `englishCoach.currentUserId` in `localStorage`.
- Refresh returns directly to the app.

- [ ] **Step 7: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: add lightweight coach user entry"
```

---

### Task 2: Navigation and Visual System

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Replace the main shell markup**

Wrap the normal app in:

```html
<div id="app-shell" class="app-shell hidden">
  <header class="topbar">
    <div class="brand-block">
      <div class="brand-title">English Coach</div>
      <div class="brand-subtitle">Daily English practice</div>
    </div>
    <nav class="main-nav">
      <button class="nav-btn active" data-page="today" onclick="showPage('today')">Today</button>
      <button class="nav-btn" data-page="practice" onclick="showPage('practice')">Practice</button>
      <button class="nav-btn" data-page="memory" onclick="showPage('memory')">Memory</button>
      <button class="nav-btn" data-page="progress" onclick="showPage('progress')">Progress</button>
    </nav>
    <div class="user-menu">
      <span id="current-user-label">-</span>
      <button class="ghost-btn" onclick="switchUser()">Switch</button>
    </div>
  </header>
  <main class="main-content">
    ...pages...
  </main>
</div>
```

Remove `Legacy` from the visible main nav.

- [ ] **Step 2: Replace color and layout CSS with a coherent system**

Use restrained tokens near the top of `<style>`:

```css
:root {
  --bg: #f6f7f9;
  --surface: #ffffff;
  --surface-muted: #f1f5f9;
  --border: #d9dee7;
  --text: #172033;
  --muted: #657083;
  --primary: #2563eb;
  --primary-hover: #1d4ed8;
  --success: #15803d;
  --warning-bg: #fff7ed;
  --error: #b42318;
  --radius: 8px;
}

body {
  background: var(--bg);
  color: var(--text);
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.app-shell { min-height: 100vh; }
.topbar {
  height: 64px;
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto minmax(180px, 1fr);
  align-items: center;
  gap: 24px;
  padding: 0 28px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.main-content { max-width: 1180px; margin: 0 auto; padding: 24px; }
.panel { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); }
.btn, .nav-btn, .ghost-btn { min-height: 38px; border-radius: 6px; font: inherit; }
```

- [ ] **Step 3: Update `showPage()` to use data attributes**

```javascript
function showPage(name) {
    document.querySelectorAll('[data-page-panel]').forEach(el => el.classList.add('hidden'));
    document.querySelector(`[data-page-panel="${name}"]`)?.classList.remove('hidden');
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.page === name);
    });
}
```

- [ ] **Step 4: Add responsive behavior**

CSS requirements:

- At widths under 760px, topbar becomes stacked or two-row.
- Main content padding reduces to 14-16px.
- Today workspace becomes one column.
- Buttons and inputs must not overlap.

- [ ] **Step 5: Manually verify navigation and layout**

Expected:

- Main nav only shows `Today`, `Practice`, `Memory`, `Progress`.
- No large saturated blue header remains.
- Buttons, inputs, and nav items align consistently.
- Mobile width does not overlap controls.

- [ ] **Step 6: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: redesign coach shell navigation"
```

---

### Task 3: Today Workspace and Auto Session

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Replace Today markup**

Create a `Today` panel:

```html
<section data-page-panel="today" id="page-today" class="page-panel">
  <div class="today-header panel">
    <div>
      <p class="eyebrow">Today</p>
      <h1 id="today-title">Build useful English sentences</h1>
      <p id="today-subtitle">Practice for 10 minutes with your coach.</p>
    </div>
    <div class="today-meta">
      <span id="today-goal-pill" class="pill">General</span>
      <span id="today-level-pill" class="pill">Level pending</span>
    </div>
  </div>
  <div class="today-grid">
    <section class="conversation-panel panel">
      <div id="coach-stream" class="coach-stream"></div>
      <div id="coach-error" class="hidden"></div>
      <div class="composer">
        <div class="mode-selector">
          <button id="coach-mode-chat" class="active" onclick="setCoachMode('CHAT')">Chat</button>
          <button id="coach-mode-fix" onclick="setCoachMode('FIX')">Fix sentence</button>
          <button id="coach-mode-drill" onclick="setCoachMode('DRILL')">Guided</button>
        </div>
        <textarea id="coach-input" placeholder="Write your English here..."></textarea>
        <div class="composer-footer">
          <span id="composer-hint">Enter sends, Shift+Enter starts a new line.</span>
          <button id="sendCoachBtn" class="btn btn-primary" onclick="submitCoachTurn()">Send</button>
        </div>
      </div>
    </section>
    <aside class="memory-side panel" id="priority-memory"></aside>
  </div>
</section>
```

- [ ] **Step 2: Initialize Today state after user entry**

```javascript
function renderUserSummary() {
    if (!appState.user) return;
    document.getElementById('current-user-label').textContent = appState.user.userCode;
    document.getElementById('today-goal-pill').textContent = appState.user.goal || 'General';
    document.getElementById('today-subtitle').textContent = `Practice for ${appState.user.dailyMinutes || 10} minutes with your coach.`;
    if (!appState.coachMessages.length) {
        appState.coachMessages = [{ id: crypto.randomUUID(), role: 'coach', text: 'Write one sentence in English. I will help you improve it.' }];
    }
    renderCoachStream();
}
```

- [ ] **Step 3: Add `ensureCoachSession()`**

```javascript
async function ensureCoachSession() {
    if (appState.coachSessionId) return appState.coachSessionId;
    const data = await apiPost(`${JAVA_BASE}/api/coach/sessions`, {
        userId: appState.user.id,
        sessionType: 'TODAY_COACH'
    });
    if (data.code !== 0) throw new Error(data.message || 'Could not start coach session');
    appState.coachSessionId = data.data.id;
    return appState.coachSessionId;
}
```

- [ ] **Step 4: Rewrite `submitCoachTurn()` for automatic session and waiting state**

```javascript
async function submitCoachTurn(retryText) {
    if (!appState.user || appState.sending) return;
    const input = document.getElementById('coach-input');
    const message = retryText || input.value.trim();
    if (!message) return;

    appState.sending = true;
    setButtonBusy('sendCoachBtn', true, 'Sending...');
    clearInlineError();
    if (!retryText) input.value = '';

    const userMessage = { id: crypto.randomUUID(), role: 'user', text: message };
    const waitingMessage = { id: crypto.randomUUID(), role: 'coach', text: '', waiting: true };
    appState.coachMessages.push(userMessage, waitingMessage);
    renderCoachStream();

    try {
        const sessionId = await ensureCoachSession();
        const data = await apiPost(`${JAVA_BASE}/api/coach/sessions/${sessionId}/turns`, {
            mode: appState.coachMode,
            message
        });
        if (data.code !== 0) throw new Error(data.message || 'Coach request failed');
        Object.assign(waitingMessage, {
            waiting: false,
            text: data.data.coachReply,
            savedNotes: data.data.savedNotes || [],
            drillSuggestion: data.data.drillSuggestion
        });
        appState.priorityMemory = (data.data.priorityMemory && data.data.priorityMemory.items) || [];
        renderPriorityMemory(appState.priorityMemory, { highlightNew: true });
        renderMemoryPage(appState.priorityMemory);
    } catch (error) {
        waitingMessage.waiting = false;
        waitingMessage.failed = true;
        waitingMessage.text = 'I could not respond. Please try again.';
        waitingMessage.retryText = message;
        showInlineError(error.message || 'Request failed');
    } finally {
        appState.sending = false;
        setButtonBusy('sendCoachBtn', false, 'Send');
        renderCoachStream();
    }
}
```

- [ ] **Step 5: Add waiting and retry rendering**

```javascript
function renderCoachStream() {
    const stream = document.getElementById('coach-stream');
    stream.innerHTML = appState.coachMessages.map(message => {
        if (message.waiting) return `<div class="coach-message coach entering"><div class="typing-dots"><span></span><span></span><span></span></div></div>`;
        const retry = message.failed && message.retryText
            ? `<button class="ghost-btn" onclick="submitCoachTurn('${escapeJs(message.retryText)}')">Retry</button>`
            : '';
        return `
            <div class="coach-message ${message.role} entering">
                <div>${escapeHtml(message.text)}</div>
                ${(message.savedNotes || []).map(renderSavedNote).join('')}
                ${message.drillSuggestion ? renderDrillSuggestion(message.drillSuggestion) : ''}
                ${retry}
            </div>`;
    }).join('');
    stream.scrollTop = stream.scrollHeight;
}
```

Also add:

```javascript
function escapeJs(value) {
    return String(value || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/\n/g, '\\n');
}
```

- [ ] **Step 6: Add animation CSS**

```css
.entering { animation: messageIn 160ms ease-out; }
@keyframes messageIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.typing-dots { display: inline-flex; gap: 4px; align-items: center; min-height: 20px; }
.typing-dots span { width: 6px; height: 6px; border-radius: 50%; background: var(--muted); animation: typingDot 1s infinite ease-in-out; }
.typing-dots span:nth-child(2) { animation-delay: 120ms; }
.typing-dots span:nth-child(3) { animation-delay: 240ms; }
@keyframes typingDot {
  0%, 80%, 100% { opacity: .35; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}
```

- [ ] **Step 7: Manually verify Today**

Expected:

- No `Start Coach` button in learner path.
- First message creates session automatically.
- Waiting dots appear before coach reply.
- Send button disables during request.
- Failed request shows retry.
- Memory panel refreshes after reply.

- [ ] **Step 8: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: redesign today coach workspace"
```

---

### Task 4: Practice Page with Guided Practice Entry

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Add Practice page markup**

```html
<section data-page-panel="practice" id="page-practice" class="page-panel hidden">
  <div class="page-heading">
    <p class="eyebrow">Practice</p>
    <h1>Choose how you want to practice</h1>
  </div>
  <div class="practice-grid">
    <article class="practice-tool panel">
      <h2>Guided Practice</h2>
      <p>Build useful sentences step by step.</p>
      <button class="btn btn-primary" onclick="startGuidedPractice()">Start guided practice</button>
    </article>
    <article class="practice-tool panel">
      <h2>Fix My Sentence</h2>
      <p>Paste one sentence and improve it.</p>
      <button class="btn" onclick="startFixPractice()">Fix a sentence</button>
    </article>
    <article class="practice-tool panel">
      <h2>Drill Memory</h2>
      <p>Practice your saved weak points.</p>
      <button class="btn" onclick="startMemoryDrill()">Choose memory</button>
    </article>
  </div>
  <section id="guided-practice-panel" class="guided-panel panel hidden"></section>
</section>
```

- [ ] **Step 2: Implement fixed Guided Practice flow**

```javascript
const guidedPractice = {
    pattern: 'I need to ...',
    prompt: 'Complete the sentence: I need to ____ the demo.',
    verbs: ['prepare', 'join', 'explain'],
    selectedVerb: null
};

function startGuidedPractice() {
    const panel = document.getElementById('guided-practice-panel');
    panel.classList.remove('hidden');
    panel.innerHTML = `
        <p class="eyebrow">Guided Practice</p>
        <h2>${guidedPractice.pattern}</h2>
        <div class="step-block">
          <h3>Step 1: Choose a verb</h3>
          <div class="choice-row">
            ${guidedPractice.verbs.map(v => `<button class="choice-btn" onclick="selectGuidedVerb('${v}')">${v}</button>`).join('')}
          </div>
        </div>
        <div class="step-block">
          <h3>Step 2: Complete the sentence</h3>
          <textarea id="guidedSentence" placeholder="I need to prepare the demo."></textarea>
          <button class="btn btn-primary" onclick="submitGuidedSentence()">Check sentence</button>
        </div>
        <div id="guided-feedback"></div>`;
}

function selectGuidedVerb(verb) {
    guidedPractice.selectedVerb = verb;
    document.getElementById('guidedSentence').value = `I need to ${verb} the demo.`;
}

async function submitGuidedSentence() {
    const sentence = document.getElementById('guidedSentence').value.trim();
    if (!sentence) return;
    showPage('today');
    setCoachMode('FIX');
    document.getElementById('coach-input').value = sentence;
    await submitCoachTurn();
}
```

- [ ] **Step 3: Implement Fix and Drill shortcuts**

```javascript
function startFixPractice() {
    showPage('today');
    setCoachMode('FIX');
    document.getElementById('coach-input').focus();
}

function startMemoryDrill() {
    showPage('memory');
}
```

- [ ] **Step 4: Manually verify Practice**

Expected:

- Practice page has three clear tools.
- Guided Practice helps construct `I need to ...`.
- Submitting guided sentence routes into coach feedback.
- Fix shortcut switches Today to Fix sentence.
- Drill shortcut points user to Memory.

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: add guided practice entry"
```

---

### Task 5: Memory and Progress Cleanup

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Redesign Memory page markup**

```html
<section data-page-panel="memory" id="page-memory" class="page-panel hidden">
  <div class="page-heading">
    <p class="eyebrow">Memory</p>
    <h1>Your learning notebook</h1>
  </div>
  <div id="memory-page-list" class="memory-list"></div>
</section>
```

- [ ] **Step 2: Rewrite memory item rendering**

```javascript
function renderMemoryItems(items) {
    if (!items || !items.length) {
        return `<div class="empty-state panel">No memory yet. Practice a few sentences and your coach will save useful patterns here.</div>`;
    }
    return items.map(item => `
        <article class="memory-card ${item.justUpdated ? 'memory-updated' : ''}">
            <div class="memory-card-header">
                <h3>${escapeHtml(item.label || '-')}</h3>
                <span class="badge ${item.recommendedAction === 'START_DRILL' ? 'badge-yellow' : 'badge-gray'}">${escapeHtml(item.recommendedAction || 'REVIEW')}</span>
            </div>
            <p class="memory-source">${escapeHtml(item.sourceText || '')}</p>
            <button class="ghost-btn" onclick="practiceMemory('${escapeJs(item.label || item.sourceText || '')}')">Practice</button>
        </article>`).join('');
}

function practiceMemory(label) {
    showPage('today');
    setCoachMode('DRILL');
    document.getElementById('coach-input').value = label ? `Practice: ${label}` : 'Practice my recent memory.';
    document.getElementById('coach-input').focus();
}
```

- [ ] **Step 3: Add memory highlight CSS**

```css
.memory-updated { animation: memoryHighlight 1400ms ease-out; }
@keyframes memoryHighlight {
  from { background: #ecfdf5; border-color: #86efac; }
  to { background: var(--surface); border-color: var(--border); }
}
```

- [ ] **Step 4: Redesign Progress page markup and rendering**

Keep existing `GET /api/coach/review`, but make the page use current user automatically. Add date inputs and a single load button.

```html
<section data-page-panel="progress" id="page-progress" class="page-panel hidden">
  <div class="page-heading">
    <p class="eyebrow">Progress</p>
    <h1>Your learning review</h1>
  </div>
  <div class="review-controls panel">
    <input type="date" id="coachReviewStart" />
    <input type="date" id="coachReviewEnd" />
    <button class="btn btn-primary" onclick="loadCoachReview()">Load review</button>
  </div>
  <div id="coach-review-content" class="review-content"></div>
</section>
```

Update `loadCoachReview()` to use `appState.user.id` instead of a visible User ID input.

- [ ] **Step 5: Implement date defaults**

```javascript
function setDefaultReviewDates() {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 7);
    document.getElementById('coachReviewStart').value = start.toISOString().slice(0, 10);
    document.getElementById('coachReviewEnd').value = end.toISOString().slice(0, 10);
}
```

- [ ] **Step 6: Manually verify Memory and Progress**

Expected:

- Memory page has no manual User ID input.
- Empty memory state is clear.
- Memory item has Practice action.
- Progress uses current user automatically.
- Review date defaults are set.

- [ ] **Step 7: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: polish memory and progress views"
```

---

### Task 6: Interaction Polish, Accessibility, and Final Verification

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Add shared busy button helper**

```javascript
function setButtonBusy(id, busy, label) {
    const button = document.getElementById(id);
    if (!button) return;
    button.disabled = busy;
    button.textContent = label;
}
```

- [ ] **Step 2: Add inline error helpers**

```javascript
function showInlineError(message) {
    const el = document.getElementById('coach-error');
    el.className = 'msg msg-error';
    el.textContent = message;
}

function clearInlineError() {
    const el = document.getElementById('coach-error');
    el.className = 'hidden';
    el.textContent = '';
}
```

- [ ] **Step 3: Ensure keyboard behavior**

Attach once during init:

```javascript
function wireKeyboardShortcuts() {
    const input = document.getElementById('coach-input');
    input.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            submitCoachTurn();
        }
    });
}
```

Call `wireKeyboardShortcuts()` from `initApp()` after DOM is ready.

- [ ] **Step 4: Remove or hide obsolete main-path controls**

Remove from visible learner path:

- Manual `coachUserId` field
- Manual `coachNewUserCode` field
- `Load` button
- `Create` button in Today header
- `Start Coach` button
- Main `Legacy` nav button
- Visible session label unless used only for internal debug

Keep legacy sections only if hidden from main navigation and not visible by default.

- [ ] **Step 5: Manual full-path verification**

With postgres, java-core, and python-agent running:

```powershell
docker compose -f docker/docker-compose.yml up -d
cd java-core
mvn spring-boot:run
cd ..\python-agent
uvicorn app.main:app --reload --port 8000
```

Verify in browser:

1. First visit shows welcome panel.
2. Continue creates user and enters Today.
3. Refresh returns to Today with same user.
4. First message sends without manual session start.
5. Coach waiting animation appears.
6. Send is disabled during request.
7. Memory side panel updates after reply.
8. Practice -> Guided Practice -> Check sentence routes to coach feedback.
9. Memory -> Practice action pre-fills drill prompt.
10. Progress loads current user's review.
11. Mobile viewport has no overlapping controls.

- [ ] **Step 6: Run backend smoke tests relevant to static route and coach client**

Python:

```powershell
cd python-agent
python -m pytest tests/test_health.py tests/test_coach_turn_routes.py -q
```

Java:

```powershell
cd java-core
mvn test -Dtest=PythonAgentClientTest
```

If Maven cannot write to default local repo in the current environment, use:

```powershell
$env:MAVEN_OPTS='-Duser.home=C:\workspace\english-coach-agent\java-core\target\maven-home'
mvn "-Dmaven.repo.local=C:\workspace\english-coach-agent\java-core\target\m2repo" test "-Dtest=PythonAgentClientTest"
```

- [ ] **Step 7: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: polish coach cockpit interactions"
```

---

## Final Handoff Checklist

- [ ] `Today`, `Practice`, `Memory`, and `Progress` are the only main learner nav items.
- [ ] Returning users do not manually load IDs.
- [ ] First coach message auto-creates a session.
- [ ] Guided Practice entry exists and works as a beginner path.
- [ ] Waiting, sending, retry, new message, and memory highlight states are visible.
- [ ] Mobile and desktop layouts are checked.
- [ ] Relevant Python and Java tests pass or blocked verification is documented with exact error output.
