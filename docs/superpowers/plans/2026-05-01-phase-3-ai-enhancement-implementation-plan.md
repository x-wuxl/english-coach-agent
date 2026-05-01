# Phase 3: AI Enhancement Implementation Plan

> **Status: COMPLETED (2026-05-01)** — All 5 tasks done, 12 python-agent tests + 77 java-core tests passing.

**Goal:** Integrate LLM capabilities into the learning loop via python-agent, using LiteLLM as the provider abstraction layer. Enable coach feedback and error explanation generation.

**Architecture:** python-agent calls LLM via LiteLLM, exposes HTTP endpoints. java-core calls python-agent's endpoints and stores results in existing DB fields (`llm_explanation_snapshot`). LLM never writes core learning state directly.

**Tech Stack:** Python 3.11+, FastAPI, LiteLLM 1.83+, Pydantic, pytest.

---

## Task 1: LiteLLM Provider Abstraction

**Delivers:** Unified LLM config, service layer, fallback routing

### Files
- Create: `python-agent/app/config.py` — env-based config (model names, API keys, base URLs)
- Create: `python-agent/app/services/llm_service.py` — LiteLLM wrapper with completion/structured/fallback
- Modify: `python-agent/pyproject.toml` — add `litellm` dependency

### Design
- Config via env vars: `LLM_DEFAULT_MODEL`, `LLM_COACH_MODEL`, `LLM_EXPLANATION_MODEL`, `LLM_API_KEY`, `LLM_API_BASE`
- `LLMService.completion()` — plain text generation
- `LLMService.structured()` — JSON/Pydantic output via `response_format`
- Graceful degradation: LLM failure returns None, caller handles fallback
- Default model: `deepseek/deepseek-chat` (cheap, fast, good at Chinese)

### Tests
- `tests/test_llm_service.py` — mock litellm, test config loading, test fallback on error

---

## Task 2: Coach Feedback Agent

**Delivers:** Generate natural language feedback after attempt/session

### Files
- Create: `python-agent/app/agents/coach_agent.py` — prompt templates + feedback generation
- Create: `python-agent/app/api/coach_routes.py` — POST /api/coach/feedback
- Create: `python-agent/app/api/dto.py` — request/response Pydantic models

### Design
- Input: attempt result (item content, user answer, correct/incorrect, mode, error type)
- Output: `{ "feedback": "...", "encouragement": "..." }`
- Prompt template: bilingual (中文解释 + English feedback), concise, coach-like tone
- Fallback: if LLM unavailable, return rule-based template message

### Tests
- `tests/test_coach_agent.py` — test with mocked LLM, test fallback

---

## Task 3: Error Explanation Agent

**Delivers:** Generate structured error explanations for wrong attempts

### Files
- Create: `python-agent/app/agents/explanation_agent.py` — prompt templates + explanation generation
- Create: `python-agent/app/api/explanation_routes.py` — POST /api/explain/error

### Design
- Input: learning item, user's wrong answer, error type
- Output: `{ "explanation": "...", "correct_usage": "...", "example": "..." }`
- Structured output via Pydantic `response_format`
- Fallback: return generic explanation template

### Tests
- `tests/test_explanation_agent.py` — test with mocked LLM, test structured output

---

## Task 4: API Integration & Main App Wiring

**Delivers:** All routes registered, config loaded, health check updated

### Files
- Modify: `python-agent/app/main.py` — register new routers
- Modify: `python-agent/.env.example` — document env vars

### Tests
- `tests/test_api_integration.py` — smoke test all endpoints

---

## Task 5: java-core Integration (Optional Stretch)

**Delivers:** java-core calls python-agent for explanations

### Files
- Modify: `java-core` StudySessionService — call python-agent after attempt submission
- Store LLM response in `attempt_log.llm_explanation_snapshot`

---

## Recommended Order
1. Task 1 (provider abstraction) — foundation
2. Task 2 (coach feedback) — first LLM feature
3. Task 3 (error explanation) — second LLM feature
4. Task 4 (wiring + integration tests)
5. Task 5 (java-core integration, optional)
