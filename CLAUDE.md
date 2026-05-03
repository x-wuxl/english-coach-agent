# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

An English learning coach split into two services:
- **java-core** (port 8080): Domain truth, persistence, scheduling, state machines. Spring Boot 3.5 + MyBatis-Plus + PostgreSQL + Flyway.
- **python-agent** (port 8000): Orchestration, model adapters, reflection/correction loops. FastAPI + uvicorn.

## Build & Run Commands

```bash
# Database
docker compose -f docker/docker-compose.yml up -d          # Postgres on 5432

# java-core
cd java-core
mvn test                                                     # all tests
mvn test "-Dtest=FlywayMigrationTest"                        # single test class
mvn test "-Dtest=UserProfileControllerTest#createUser"       # single test method
mvn spring-boot:run                                          # start on :8080

# python-agent
cd python-agent
pip install -e ".[dev]"                                      # install deps
python -m pytest -v                                          # all tests
python -m pytest tests/test_health.py -v                     # single test file
python -m pytest tests/test_health.py::test_fn -k "name"     # single test
uvicorn app.main:app --reload --port 8000                    # start on :8000
```

## Architecture

### java-core layered structure

```
api/          → Controllers, request/response DTOs. No business logic.
application/  → Service layer. Orchestrates domain + infrastructure.
domain/       → Pure domain models (records). No framework dependencies.
infrastructure/persistence/ → MyBatis-Plus mappers + DO (data objects).
common/       → Enums, exceptions, response wrappers, config.
```

Key conventions:
- Domain models are Java records. Persistence objects are Lombok `@Getter/@Setter` classes with `@TableName`.
- Service layer manually maps between DO ↔ domain ↔ response (no MapStruct).
- All API responses wrapped in `BaseResponse<T>` with `code=0` for success.
- Errors use `BusinessException(ErrorCodeEnum)` → caught by `GlobalExceptionHandler`.
- DB migrations in `src/main/resources/db/migration/` (Flyway, `V{n}__desc.sql`).
- IDs are auto-increment `bigint`. MyBatis-Plus configured with `id-type: ASSIGN_UUID` (but schema uses identity).
- Enums stored as `varchar` strings in DB, mapped via `Enum.valueOf()`.
- Tests use H2 for Flyway migration tests and Testcontainers for integration tests.

### python-agent structure

```
app/
  main.py               → FastAPI app, registers routers + tracing middleware
  config.py              → Pydantic settings (env-based LLM config)
  api/
    health_routes.py     → GET /health
    coach_routes.py      → POST /api/coach/feedback
    explanation_routes.py→ POST /api/explain/error
    reflection_routes.py → POST /api/reflect/session
    harness_routes.py    → GET /api/harness/recordings
    reminder_routes.py   → POST/GET /api/reminders
    ui_routes.py         → GET / (static HTML)
    dto.py               → Request/response Pydantic models
  agents/
    coach_agent.py       → Coach feedback generation
    explanation_agent.py → Error explanation generation
    reflection_agent.py  → Session reflection generation
  services/
    llm_service.py       → LiteLLM wrapper with fallback + tracing
    correction_service.py→ LLM output validation + retry
    reminder_service.py  → In-memory reminder store
  middleware/
    tracing.py           → Request tracing (trace_id + timing)
  harness/
    recorder.py          → Record interactions to JSON
    replayer.py          → Load/replay recordings
  static/
    index.html           → Web UI (dashboard/study/mastery/review)
tests/                   → pytest tests (22 tests)
```

API endpoints (python-agent):
- `POST /api/coach/feedback` — coach feedback per attempt
- `POST /api/explain/error` — structured error explanation
- `POST /api/reflect/session` — session reflection
- `GET /api/harness/recordings` — list recorded interactions
- `GET /api/reminders` — query reminders
- `GET /` — Web UI

Config via `.env`: `LLM_DEFAULT_MODEL`, `LLM_API_KEY`, `LLM_FALLBACK_MODEL`, `TRACING_ENABLED`.

## Implementation Status

**Phase 1 (Foundation):** Complete — user profile CRUD, content seed data, Flyway migrations, H2 test infra.

**Phase 2 (Core Learning Loop):** Complete — 77 tests, full learning cycle wired:
- `POST /api/placement/assess` — placement scoring, level assignment
- `POST /api/plans/daily:generate` / `GET /api/plans/daily` — daily plan with load policy, review priority, new item selection
- `POST /api/sessions/start` / `POST /api/sessions/{id}/attempts` / `POST /api/sessions/{id}/complete` / `GET /api/sessions/{id}` — study session lifecycle
- `GET /api/mastery` / `GET /api/mastery/due-review` — mastery state queries
- `POST /api/reviews/weekly:generate` / `GET /api/reviews/weekly` — weekly review aggregation

**Phase 3 (AI Enhancement):** Complete — LiteLLM provider abstraction, coach feedback agent, error explanation agent, java-core ↔ python-agent integration.

**Phase 4 (Reliability):** Complete — session reflection agent, correction loop, model fallback chain, request tracing, harness replay.

**Phase 5 (Experience):** In progress — Web UI functional but UX needs improvement:
- Dashboard: mastery stats overview
- Placement: 10-question interactive quiz (vocab/grammar/reading/output)
- Study: interactive session with daily plan items, LLM-based correctness judgment, coach feedback
- Mastery: mastery state table with status badges
- Weekly Review: weekly aggregation view

## Web UI

Served by python-agent at `GET /` (static/index.html). Calls java-core (:8080) for domain APIs and python-agent (:8000) for LLM features.

Flow: Create User → Placement Assessment → Study Session → Dashboard/Mastery/Review.

Key integration points:
- `POST /api/coach/feedback` — LLM judges correctness + gives feedback (requires `LLM_API_KEY`, `LLM_API_BASE`, `LLM_DEFAULT_MODEL` in `.env`)
- Study items loaded from daily plan, meaning_zh included for frontend display
- CORS enabled via `WebConfig.java` for cross-origin requests from python-agent to java-core

## LLM Configuration

python-agent uses LiteLLM. Config via `.env`:
- `LLM_DEFAULT_MODEL` — model ID (e.g. `openai/mimo-v2.5-pro`, `deepseek/deepseek-chat`)
- `LLM_API_KEY` — API key
- `LLM_API_BASE` — custom endpoint URL
- `LLM_FALLBACK_MODEL` — fallback model when primary fails
- `LLM_COACH_MODEL`, `LLM_EXPLANATION_MODEL`, `LLM_REFLECTION_MODEL` — per-task overrides

For reasoning models (DeepSeek-R1, MiMo, etc.), set `max_tokens` ≥ 1024 to allow space for both reasoning and response content.

## Current Limitations

- **Learning data**: Only 20 seed items (V4). No real curriculum content.
- **Web UI/UX**: Functional but basic. Needs better visual design, responsive layout, and smoother交互 flow.
- **No authentication**: All APIs are open.
- **No real-time progress tracking**: Session items not persisted in plan after completion.

## Core Domain

The learning loop: placement → daily planning → study session → mastery update → weekly review.

Key domain classes: `PlacementScorer`, `DailyLoadPolicy`, `ReviewPriorityCalculator`, `NewItemSelectionPolicy`, `ScoreUpdatePolicy`, `MasteryStateMachine`, `NextReviewPolicy`, `WeeklyReviewAggregator`.

Key enums: `GoalType`, `MasteryStatus`, `PlanStatus`, `SessionStatus`, `AttemptResult`, `LearningItemType`, `ItemRole`, `SessionType`, `PlanType`.

DB migrations: V1 (user_profile) → V2 (learning_item, mastery_state, study_session, attempt_log) → V3 (daily_plan_snapshot, daily_plan_item, weekly_review_snapshot) → V4 (seed learning items).

Detailed specs live in `docs/superpowers/specs/` and plans in `docs/superpowers/plans/`.

## Principles

1. The repo is the single source of truth. Decisions that affect code must land as versioned artifacts.
2. Encode taste as machine-verifiable rules (linters, tests, CI), not prose.
3. Execution plans with progress logs are versioned artifacts stored in repo.
4. When stuck, fix the environment (missing context, tools, constraints), not by trying harder.

## Env Vars

java-core: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `PYTHON_AGENT_URL`, `PYTHON_AGENT_ENABLED`. Defaults in `application.yaml`.

python-agent: `LLM_DEFAULT_MODEL`, `LLM_API_KEY`, `LLM_API_BASE`, `LLM_FALLBACK_MODEL`, `LLM_TIMEOUT`, `LLM_MAX_RETRIES`, `TRACING_ENABLED`. See `.env.example`.
