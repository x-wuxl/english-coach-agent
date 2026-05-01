# english-coach-agent

API-first English learning coach for practicing modern Agent engineering.

## What this project is

This project is a personal-use MVP for learning how to build reliable AI-driven systems.
It focuses on a full learning loop:

- placement
- daily planning
- study session
- mastery update
- weekly review

The architecture is intentionally split into two parts:

- `java-core`: owns domain truth, rules, state machine, scheduling, persistence
- `python-agent`: owns orchestration, model adapters, reflection, correction, harness replay

## Why this project exists

This repo is used to practice:

- Agent workflow design
- rule + LLM hybrid architecture
- reliable memory
- reflection / correction loops
- multi-provider model integration
- Harness Engineering

## Current docs

- Product requirement: `english-coach-design.md`
- Technical design: `docs/superpowers/specs/2026-03-25-english-coach-agent-design.md`
- Implementation plan: `docs/superpowers/plans/2026-03-25-english-coach-agent-mvp-implementation-plan.md`

## Planned structure

```text
english-coach-agent/
├─ java-core/
├─ python-agent/
├─ docker/
├─ seeds/
└─ docs/
```

## Next milestone

Build the MVP loop first:

1. user profile
2. placement assessment
3. daily plan generation
4. study session
5. mastery update
6. weekly review
7. reflection / correction
8. harness replay

## Phase 1 local smoke test

1. Start PostgreSQL with Docker Compose
2. Start java-core on port 8080
3. Call GET /api/health
4. Create a user with POST /api/users
5. Read the user back with GET /api/users/{userId}
6. Start python-agent on port 8000
7. Call GET /health

## Phase 1 run commands

Start PostgreSQL:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Run java-core tests:

```bash
cd java-core
mvn test "-Dtest=FlywayMigrationTest,HealthCheckControllerTest,UserProfileControllerTest"
```

Start java-core:

```bash
cd java-core
mvn spring-boot:run
```

Run python-agent tests:

```bash
cd python-agent
python -m pytest -v
```

Start python-agent:

```bash
cd python-agent
uvicorn app.main:app --reload --port 8000
```
