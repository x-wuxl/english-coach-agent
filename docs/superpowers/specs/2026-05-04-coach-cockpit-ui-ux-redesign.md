# Coach Cockpit UI/UX Redesign

## Context

The current `python-agent/app/static/index.html` experience exposes too much implementation detail to the learner. Users must manually create or load a user ID, manually start a coach session, and choose between new coach features and old legacy modules. The layout, spacing, colors, controls, and loading states make the app feel like an API test page rather than a daily English learning product.

The next product step is a UI/UX redesign that keeps the current Java and Python services mostly intact while making the app usable for daily learning. The redesign also introduces a visible path for lower-level learners through Guided Practice, so the app is not limited to open-ended conversation.

## Goals

1. Make the first-run experience clear and low-friction.
2. Remove manual `Load User` and `Start Coach` steps from normal use.
3. Make `Today` the primary learning workspace.
4. Add a `Practice` area with a Guided Practice entry for beginners.
5. Present Memory as an actionable learning notebook, not a raw data panel.
6. Present Progress as a clear learning review, not just API output.
7. Hide Legacy modules from the main learner navigation.
8. Add interaction feedback: waiting, sending, retry, new message, and new memory states.
9. Establish a quieter, more coherent visual system.

## Non-Goals

1. No full username/password authentication in this phase.
2. No JWT, Spring Security, or permission model in this phase.
3. No complete rewrite of Java or Python APIs.
4. No deletion of legacy backend or old learning APIs.
5. No complex adaptive curriculum engine in the first Guided Practice pass.

## Recommended Approach

Use a lightweight frontend redesign over the existing static HTML app. Keep the backend surface mostly unchanged and rely on `localStorage` for the current user context.

This gives the largest UX improvement without taking on full authentication, frontend framework migration, or backend architecture changes.

## Information Architecture

Main navigation becomes:

```text
Today
Practice
Memory
Progress
```

Legacy learner-facing entries are removed from the main navigation:

```text
Legacy
Dashboard
Study
Placement
Mastery
Weekly Review
```

Legacy code may remain in `index.html` temporarily, but it should not appear in the normal learner path. If needed, it can be exposed later behind a low-priority developer/debug entry.

## First-Run User Flow

When no saved user exists in `localStorage`, the app shows a welcome panel:

```text
Welcome to English Coach

User code
[ wuxl ]

Goal
[ General / Workplace ]

Daily minutes
[ 10 ]

[ Continue ]
```

Behavior:

1. User enters a user code, goal, and daily minutes.
2. Frontend calls `POST /api/users`.
3. On success, frontend stores:

```text
currentUserId
currentUserCode
currentUserGoal
currentDailyMinutes
```

4. App transitions to `Today`.
5. Future visits auto-load this user from `localStorage`.
6. A `Switch user` control clears or replaces the saved user.

If the user already exists but `POST /api/users` returns a duplicate error, the current API has no user-code lookup endpoint. For this phase, the user can switch using a previously saved local user or create a new unique user code. A later backend improvement can add `GET /api/users/by-code/{userCode}`.

## Today Page

`Today` is the main learning workspace. It must not expose session mechanics to normal users.

Desktop layout:

```text
Top bar:
English Coach        Today Practice Memory Progress        wuxl

Today header:
Today's focus
Build useful work sentences
10 min plan

Main workspace:
Coach conversation                         Priority Memory
message stream                             3-5 memory items
input area                                 Practice buttons
```

Behavior:

1. Input is available immediately.
2. If there is no active coach session, the first send automatically calls `POST /api/coach/sessions`.
3. The session ID is stored in memory for the current page session.
4. Session codes are not shown in the main learner UI.
5. Messages append optimistically, then show a coach waiting indicator.
6. On success, the waiting indicator is replaced by the coach response.
7. On failure, the user message shows retry affordance and the input becomes available again.
8. Priority Memory refreshes after each coach turn.

Mode controls remain, but their labels should be learner-oriented:

```text
Chat
Fix sentence
Guided
```

Internal API values can remain:

```text
CHAT
FIX
DRILL
```

## Practice Page

`Practice` gives non-advanced learners a structured path instead of a blank chat box.

The page contains three primary tools:

```text
Guided Practice
Fix My Sentence
Drill Memory
```

### Guided Practice

Guided Practice is the beginner-friendly path. The first pass can use a small fixed pattern flow while the backend curriculum evolves.

Example:

```text
Pattern: I need to ...

Step 1: Choose a verb
prepare / join / explain

Step 2: Complete the sentence
I need to ____ the demo.

Step 3: Make your own sentence

Step 4: Coach feedback
```

Purpose:

1. Reduce blank-page anxiety for A0-A2 learners.
2. Teach sentence frames before free conversation.
3. Convert practice results into saved memory where useful.

### Fix My Sentence

This is for learners who can write simple sentences but need correction.

Flow:

1. User enters one sentence.
2. App submits it as coach mode `FIX`.
3. Coach returns a better sentence and brief explanation.
4. User is prompted to write the improved version again.

### Drill Memory

This starts from a saved memory item.

Flow:

1. User chooses a memory item.
2. App switches to a drill mode using that memory label/context.
3. Coach gives short targeted practice.

## Memory Page

Memory should feel like an actionable notebook.

Each memory item should show:

```text
need to + verb
You wrote: I need prepare the demo.
Better: I need to prepare the demo.
Seen 3 times
[Practice]
```

Memory groups:

```text
Error Patterns
Expression Gaps
```

Empty state:

```text
No memory yet. Practice a few sentences and your coach will save useful patterns here.
```

## Progress Page

Progress should answer three learner questions:

1. What did I practice?
2. What problems repeat?
3. What should I practice next?

Initial content:

```text
Conversation turns
New memory count
Top repeated problems
Improved expressions
Next practice suggestion
```

The existing `GET /api/coach/review` can power the first version.

## Visual System

Style direction: quiet, focused learning tool.

Guidelines:

1. Use a near-white or light gray app background.
2. Avoid large saturated blue headers.
3. Use one restrained primary color for key actions.
4. Use semantic colors only for status: success, warning, error.
5. Keep buttons, inputs, and segmented controls aligned and consistent.
6. Use cards only for real panels or repeated items.
7. Do not nest cards inside cards.
8. Keep border radius at 8px or less.
9. Conversation area and input area must have stable dimensions.
10. Text must not overflow buttons, cards, or panels on mobile or desktop.

## Interaction States and Animation

Animations must communicate state, not decorate the page.

Required states:

1. Coach waiting indicator: three subtle typing dots.
2. Sending state: Send button disabled while request is in flight.
3. New message: slight fade and vertical slide.
4. New memory: short highlight on newly added memory item.
5. Page change: light fade transition.
6. Failure: inline failed state with retry action.
7. Empty states: calm placeholder text with a clear next action.

Avoid large decorative animations, gradient backgrounds, or marketing-style hero layouts.

## API Usage

Reuse existing endpoints where possible:

```text
POST /api/users
GET /api/users/{userId}
POST /api/coach/sessions
POST /api/coach/sessions/{sessionId}/turns
GET /api/memory/priority?userId={userId}
GET /api/coach/review?userId={userId}&startDate={date}&endDate={date}
```

Possible follow-up endpoint, not required for the first redesign:

```text
GET /api/users/by-code/{userCode}
```

## Error Handling

1. Network failure shows an inline retry state.
2. Java backend unavailable shows a top-level service status message.
3. Python-agent failure should surface as a coach response failure, not a silent fallback if possible.
4. Duplicate user-code errors should explain that the user code already exists.
5. Missing user context should return the app to the welcome panel.

## Testing and Verification

Manual verification:

1. First visit shows welcome panel.
2. Creating a user stores current user in `localStorage`.
3. Refresh returns to `Today` without manual load.
4. First message auto-creates a coach session.
5. Coach waiting animation appears while response is pending.
6. Failed message can be retried.
7. Memory refreshes after a coach turn.
8. Practice page shows Guided Practice, Fix My Sentence, and Drill Memory.
9. Memory empty and populated states both render cleanly.
10. Progress can load a review for the current user.
11. Mobile layout does not overlap controls or text.

Automated checks where practical:

1. Existing Java tests remain green.
2. Existing Python route tests remain green.
3. Add or update lightweight frontend smoke checks if a browser test harness is introduced.

## Acceptance Criteria

1. A new user can start using the app without manually typing user IDs after first setup.
2. A returning user lands directly in the main learning workspace.
3. The user can send a coach message without pressing `Start Coach`.
4. The main navigation only shows `Today`, `Practice`, `Memory`, and `Progress`.
5. `Guided Practice` is visible as a beginner-friendly path.
6. Conversation waiting, sending, and failure states are visible and understandable.
7. The UI has consistent spacing, alignment, button styling, and color usage.
8. Legacy modules are not part of the default learner path.
