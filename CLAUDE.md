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
  main.py           → FastAPI app, includes routers
  api/health_routes → Health check endpoint
tests/              → pytest tests using httpx TestClient
```

## Core Domain (from design docs)

The learning loop: placement → daily planning → study session → mastery update → weekly review.

Key enums: `GoalType`, `MasteryStatus`, `PlanStatus`, `SessionStatus`, `AttemptResult`, `LearningItemType`, `ItemRole`, `SessionType`, `PlanType`.

Detailed specs live in `docs/superpowers/specs/` and plans in `docs/superpowers/plans/`.

## Principles

1. The repo is the single source of truth. Decisions that affect code must land as versioned artifacts.
2. Encode taste as machine-verifiable rules (linters, tests, CI), not prose.
3. Execution plans with progress logs are versioned artifacts stored in repo.
4. When stuck, fix the environment (missing context, tools, constraints), not by trying harder.

## Env Vars

See `.env.example`. Defaults in `application.yaml`: `POSTGRES_HOST=localhost`, `POSTGRES_PORT=5432`, `POSTGRES_DB=english_coach`, `POSTGRES_USER=coach`, `POSTGRES_PASSWORD=coach`.
