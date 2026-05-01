# Phase 4 & 5: Reliability Enhancement + Experience Extension

> **Status: IN PROGRESS**

## Phase 4: Reliability Enhancement

### Task 1: Session Reflection (python-agent)
- POST /api/reflect/session — generate structured reflection after session
- Input: session stats, attempt list, mastery changes
- Output: { error_summary, pattern_insight, next_session_suggestion }
- File: `app/agents/reflection_agent.py`, `app/api/reflection_routes.py`

### Task 2: Correction Loop (python-agent)
- Validate LLM structured output against expected schema
- Retry once with simpler prompt on failure
- Fallback to rule-based default if still fails
- File: `app/services/correction_service.py`

### Task 3: Model Fallback Chain (python-agent)
- Configure primary + fallback models per task
- LiteLLM Router with fallbacks
- File: update `app/config.py`, `app/services/llm_service.py`

### Task 4: Request Tracing (python-agent)
- Middleware to log request/response with trace_id
- Include trace_id in LLM calls for debugging
- File: `app/middleware/tracing.py`

### Task 5: Harness Replay (python-agent)
- Record session input/output to JSON file
- Replay recorded session for regression testing
- File: `app/harness/recorder.py`, `app/harness/replayer.py`
- Route: POST /api/harness/record, POST /api/harness/replay

## Phase 5: Experience Extension

### Task 6: Web UI
- Static HTML/JS/CSS served by python-agent
- Pages: dashboard, study session, mastery view, weekly review
- File: `python-agent/app/static/` + `python-agent/app/api/ui_routes.py`

### Task 7: Reminder Adapter
- Interface + in-memory implementation
- Schedule reminders based on next_review_at
- File: `python-agent/app/services/reminder_service.py`
