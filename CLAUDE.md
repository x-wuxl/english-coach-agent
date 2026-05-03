# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

An English learning coach split into two services:
- **java-core** (port 8080): Domain truth, persistence, scheduling, state machines. Spring Boot 3.5 + MyBatis-Plus + PostgreSQL + Flyway.
- **python-agent** (port 8000): Orchestration, model adapters, reflection/correction loops. FastAPI + uvicorn.

## Build & Run

```bash
# java-core
cd java-core && mvn test                    # all tests
mvn test "-Dtest=FlywayMigrationTest"       # single test
mvn spring-boot:run                         # start on :8080

# python-agent
cd python-agent && pip install -e ".[dev]"
python -m pytest -v                         # all tests
uvicorn app.main:app --reload --port 8000   # start on :8000
```

## Architecture

java-core: `api/` (controllers) → `application/` (services) → `domain/` (pure records) → `infrastructure/persistence/` (mappers). All responses wrapped in `BaseResponse<T>`. Errors via `BusinessException(ErrorCodeEnum)`. DB migrations in `src/main/resources/db/migration/`.

python-agent: FastAPI routes → agents (coach/explanation/reflection) → services (llm_service, correction_service). Config via `.env` with Pydantic Settings. LiteLLM for model abstraction with fallback chain.

Cross-service: java-core calls python-agent via `PythonAgentClient` (RestClient). Web UI served by python-agent at `GET /`, calls both services. CORS enabled via `WebConfig.java`.

## Deep Docs

Specs live in `docs/superpowers/specs/`:
- `english-coach-agent-design.md` — overall architecture and phase roadmap
- `java-core-db-schema.md` — database schema (10 tables)
- `java-core-api-contracts.md` — API contracts (18 endpoints)
- `java-core-domain-rules.md` — domain business logic rules

## LLM Config

python-agent uses LiteLLM. Config via `.env`:
- `LLM_DEFAULT_MODEL` — e.g. `openai/mimo-v2.5-pro`, `deepseek/deepseek-chat`
- `LLM_API_KEY`, `LLM_API_BASE` — endpoint credentials
- `LLM_FALLBACK_MODEL` — fallback when primary fails
- `LLM_COACH_MODEL`, `LLM_EXPLANATION_MODEL`, `LLM_REFLECTION_MODEL` — per-task overrides

For reasoning models (DeepSeek-R1, MiMo, etc.), set `max_tokens` ≥ 1024.

## Current Status

Phase 1-4 complete. Phase 5 in progress — Web UI functional but UX needs improvement. Only 20 seed learning items (V4). No authentication.

## Principles

1. 代码仓库是唯一的记录系统：不在 repo 里的知识对智能体不存在。讨论、脑中决策、外部文档——如果影响开发，必须落地为 repo 内的 versioned artifact
2. 本文件是地图，不是百科全书：保持 ~100 行，指向 `docs/` 深层。每层只暴露本层信息 + 下一步导航
3. 把品味编码为规则：优先用 linter、结构测试、CI 检查来强制约束，而非自然语言指令。可机械验证 > 散文指南
4. 计划是一等工件：执行计划带进度日志，versioned 并集中存放于 docs/exec-plans/
5. 持续垃圾回收：技术债以小额持续偿还，不攒到大规模清理。
6. 卡住时修环境，不是更用力：智能体遇到困难时，问"缺什么上下文、工具或约束"，然后补进 repo
