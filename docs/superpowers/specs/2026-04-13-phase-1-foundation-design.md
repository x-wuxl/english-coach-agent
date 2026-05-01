# Phase 1 Foundation Design

Date: 2026-04-13
Status: approved-for-planning
Scope: english-coach-agent

## 1. Goal

This phase does not implement the full English coach MVP. It focuses on a stable foundation plus one minimal business slice.

This phase must deliver:

- Local PostgreSQL runtime and java-core datasource connectivity
- Flyway migration execution in java-core
- Existing java-core response envelope, exception handling, and health check kept intact
- Minimal user profile creation and read flow in java-core
- Minimal FastAPI skeleton and health endpoint in python-agent
- In-repo documentation of phase scope, module boundaries, and completion criteria

## 2. Non-Goals

This phase explicitly excludes:

- Placement assessment
- Daily plan generation
- Study session flow
- Mastery state updates
- Weekly review
- Real LLM provider integration
- Reflection or correction loop
- Harness replay
- Web frontend

The repo is still in a stage where design maturity is ahead of implementation maturity. A stable runtime, schema, service skeleton, and one minimal API are more valuable than premature partial workflow code.

## 3. Delivery Order

This phase should move in sequence instead of parallel feature pushes:

1. Complete PostgreSQL plus java-core schema and migration
2. Implement the first java-core APIs
3. Build the minimal python-agent service skeleton

Reasons:

- python-agent depends on java-core contracts
- java-core business code depends on stable persistence
- The main goal of this phase is low rework, not broad surface progress

## 4. Module Boundaries

### 4.1 java-core

Phase 1 keeps four active layers:

- api
  Handles HTTP mapping, validation, and response DTOs. No domain rules live here.
- application
  Handles use-case orchestration. Phase 1 only needs user profile create and read services.
- domain
  Holds only the minimum concepts needed now. Complex rule modules are deferred.
- infrastructure
  Handles persistence, database migration, and config.

### 4.2 python-agent

Phase 1 keeps only a runnable skeleton:

- app/main.py
  Application entrypoint and router registration.
- app/api/health_routes.py
  Health endpoint.
- app/adapters/
  Reserved for future java_core_client work.
- tests/
  Health endpoint coverage only.

### 4.3 Database

The broader schema design already exists in repo docs, but Phase 1 only migrates the table required by the current scope: user_profile.

All other tables remain in specification documents and stay out of SQL for now to reduce schema churn.

## 5. Data Flow And API Scope

### 5.1 java-core minimal flow

The minimal business path for this phase is:

1. Call the health endpoint to confirm service readiness
2. Call the create-user endpoint with the minimum profile fields
3. Controller validates input and calls the application service
4. Application service persists user_profile
5. Service returns the standard response envelope
6. Read the user back through a query endpoint to verify persistence

### 5.2 java-core endpoints

This phase implements only:

- GET /api/health
- POST /api/users
- GET /api/users/{userId}

POST /api/users accepts only the minimum field set:

- userCode
- goal
- dailyMinutes
- studyStartTime
- reviewTime

This phase does not implement:

- PUT /api/users/{userId}
- Pagination endpoints
- Richer profile fields
- Any workflow APIs beyond user creation and read

### 5.3 python-agent endpoints

This phase implements only:

- GET /health

If useful for later structure, a placeholder location for adapters/java_core_client.py may be created, but no real cross-service integration is required in this phase.

## 6. Test Strategy

This phase should cover at least:

- java-core Spring Boot context load test
- java-core health endpoint test
- java-core user creation endpoint test
- java-core user read endpoint test
- java-core request validation failure test
- java-core migration verification test
- python-agent health endpoint test

The point is not test count. The point is to prove:

- Runtime starts correctly
- Schema applies correctly
- The minimal read and write path works
- The service skeleton is real, not a stub-only shell

## 7. Completion Criteria

This phase is complete when all of the following are true:

- PostgreSQL starts through Docker
- java-core connects to the database and runs Flyway migrations
- user_profile is created by migration
- POST /api/users persists a user record
- GET /api/users/{userId} returns the stored record
- GET /api/health returns successfully
- python-agent starts and GET /health returns HTTP 200
- Relevant automated tests pass

## 8. Follow-Up

After this phase, the next phase can extend into:

- Additional user profile fields
- Placement assessment
- Daily plan generation
- python-agent integration with java-core

This phase should not add half-implemented code for those capabilities. It should only leave clean boundaries for later work.
