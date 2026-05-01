# Phase 1 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Build the phase-1 foundation slice: PostgreSQL runtime, Flyway-backed user profile persistence in java-core, and a minimal runnable python-agent skeleton.

**Architecture:** Implement this phase in strict sequence. First stabilize local runtime and the user_profile schema, then add the minimal java-core create/read user flow, and finally add the smallest possible FastAPI service with a health endpoint. Keep all workflow features out of scope.

**Tech Stack:** Docker Compose, PostgreSQL 16, Java 17, Spring Boot 3, Flyway, MyBatis-Plus, JUnit 5, Testcontainers, Python 3.12, FastAPI, pytest.

---

## 0. File Structure Map

### Runtime and docs
- Modify: README.md
- Modify: .env.example
- Modify: docker/docker-compose.yml
- Create: docs/superpowers/specs/2026-04-13-phase-1-foundation-design.md
- Create: docs/superpowers/plans/2026-04-13-phase-1-foundation-implementation-plan.md

### Java Core
- Modify: java-core/pom.xml
- Modify: java-core/src/main/resources/application.yaml
- Create: java-core/src/main/resources/db/migration/V1__create_user_profile.sql
- Create: java-core/src/main/java/com/wuxl/englishcoach/api/user/UserProfileController.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/application/user/UserProfileService.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/domain/user/UserProfile.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileDO.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileMapper.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/api/user/dto/CreateUserProfileRequest.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/api/user/dto/UserProfileResponse.java
- Create: java-core/src/test/java/com/wuxl/englishcoach/api/health/HealthCheckControllerTest.java
- Create: java-core/src/test/java/com/wuxl/englishcoach/api/user/UserProfileControllerTest.java
- Create: java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/FlywayMigrationTest.java

### Python Agent
- Create: python-agent/pyproject.toml
- Create: python-agent/app/main.py
- Create: python-agent/app/api/health_routes.py
- Create: python-agent/tests/test_health.py

---

### Task 1: Stabilize local runtime documentation and database container

**Files:**
- Modify: README.md
- Modify: .env.example
- Modify: docker/docker-compose.yml

- [ ] **Step 1: Write the failing local smoke checklist into README**

```md
## Phase 1 local smoke test
1. Start PostgreSQL with Docker Compose
2. Start java-core on port 8080
3. Call GET /api/health
4. Create a user with POST /api/users
5. Read the user back with GET /api/users/{userId}
6. Start python-agent on port 8000
7. Call GET /health
```

- [ ] **Step 2: Keep docker compose focused on PostgreSQL only**

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: english_coach
      POSTGRES_USER: coach
      POSTGRES_PASSWORD: coach
    ports:
      - "5432:5432"
```

- [ ] **Step 3: Update .env.example with the minimum phase-1 config**

```env
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=english_coach
POSTGRES_USER=coach
POSTGRES_PASSWORD=coach
JAVA_CORE_BASE_URL=http://localhost:8080
PYTHON_AGENT_BASE_URL=http://localhost:8000
```

- [ ] **Step 4: Start PostgreSQL and verify the container is up**

Run: docker compose -f docker/docker-compose.yml up -d
Expected: PostgreSQL container starts without errors

- [ ] **Step 5: Commit**

```bash
git add README.md .env.example docker/docker-compose.yml docs/superpowers/specs/2026-04-13-phase-1-foundation-design.md docs/superpowers/plans/2026-04-13-phase-1-foundation-implementation-plan.md
git commit -m "docs: define phase-1 foundation slice"
```

### Task 2: Add the user_profile migration and verify Flyway

**Files:**
- Create: java-core/src/main/resources/db/migration/V1__create_user_profile.sql
- Create: java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/FlywayMigrationTest.java
- Modify: java-core/pom.xml
- Modify: java-core/src/main/resources/application.yaml

- [ ] **Step 1: Write the failing migration test first**

```java
@Test
void flywayShouldCreateUserProfileTable() {
    Integer count = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables where table_name = 'user_profile'",
            Integer.class
    );
    assertThat(count).isEqualTo(1);
}
```

- [ ] **Step 2: Run the migration test to confirm it fails before the SQL exists**

Run: cd java-core && .\mvnw test --tests FlywayMigrationTest
Expected: FAIL because user_profile has not been migrated yet

- [ ] **Step 3: Add the minimal migration for user_profile**

```sql
create table user_profile (
    id bigint generated by default as identity primary key,
    user_code varchar(64) not null,
    goal varchar(32) not null,
    daily_minutes int not null,
    study_start_time varchar(5),
    review_time varchar(5),
    status varchar(16) not null default 'ACTIVE',
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_user_profile_user_code unique (user_code),
    constraint chk_user_profile_daily_minutes check (daily_minutes between 5 and 180)
);
```

- [ ] **Step 4: Run the migration test again**

Run: cd java-core && .\mvnw test --tests FlywayMigrationTest
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/pom.xml java-core/src/main/resources/application.yaml java-core/src/main/resources/db/migration java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence
git commit -m "feat: add phase-1 user profile migration"
```

### Task 3: Add health endpoint test coverage in java-core

**Files:**
- Create: java-core/src/test/java/com/wuxl/englishcoach/api/health/HealthCheckControllerTest.java

- [ ] **Step 1: Write the failing health controller test**

```java
@Test
void shouldReturnOkFromHealthEndpoint() throws Exception {
    mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("ok"))
            .andExpect(jsonPath("$.data").value("ok"));
}
```

- [ ] **Step 2: Run the test to verify the existing endpoint contract**

Run: cd java-core && .\mvnw test --tests HealthCheckControllerTest
Expected: PASS or small test wiring fixes needed

- [ ] **Step 3: If needed, align the test slice configuration without changing the endpoint behavior**

```java
@WebMvcTest(HealthCheckController.class)
class HealthCheckControllerTest {
}
```

- [ ] **Step 4: Re-run the health test**

Run: cd java-core && .\mvnw test --tests HealthCheckControllerTest
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/test/java/com/wuxl/englishcoach/api/health
git commit -m "test: cover java core health endpoint"
```

### Task 4: Implement the minimal user profile create and read API

**Files:**
- Create: java-core/src/main/java/com/wuxl/englishcoach/api/user/UserProfileController.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/api/user/dto/CreateUserProfileRequest.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/api/user/dto/UserProfileResponse.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/application/user/UserProfileService.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/domain/user/UserProfile.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileDO.java
- Create: java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileMapper.java
- Create: java-core/src/test/java/com/wuxl/englishcoach/api/user/UserProfileControllerTest.java

- [ ] **Step 1: Write the failing controller tests first**

```java
@Test
void shouldCreateUserProfile() throws Exception {
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userCode": "u_001",
                  "goal": "GENERAL",
                  "dailyMinutes": 20,
                  "studyStartTime": "20:00",
                  "reviewTime": "12:30"
                }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.userCode").value("u_001"));
}

@Test
void shouldGetUserProfileById() throws Exception {
    mockMvc.perform(get("/api/users/{userId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
}
```

- [ ] **Step 2: Run the user API tests to confirm they fail before implementation**

Run: cd java-core && .\mvnw test --tests UserProfileControllerTest
Expected: FAIL with missing controller or beans

- [ ] **Step 3: Implement the minimal create and get flow**

Controller endpoints:

- POST /api/users
- GET /api/users/{userId}

Request validation rules:

- userCode required, max 64
- goal required
- dailyMinutes between 5 and 180
- studyStartTime optional, format HH:mm
- reviewTime optional, format HH:mm

Service behavior:

- map request to a minimal domain object
- persist through UserProfileMapper
- map persistence result to UserProfileResponse
- throw BusinessException with existing error codes when user is not found

- [ ] **Step 4: Add one validation failure test and run the full user API test class**

Run: cd java-core && .\mvnw test --tests UserProfileControllerTest
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/user java-core/src/main/java/com/wuxl/englishcoach/application/user java-core/src/main/java/com/wuxl/englishcoach/domain/user java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user java-core/src/test/java/com/wuxl/englishcoach/api/user
git commit -m "feat: add phase-1 user profile api"
```

### Task 5: Bootstrap the python-agent health service

**Files:**
- Create: python-agent/pyproject.toml
- Create: python-agent/app/main.py
- Create: python-agent/app/api/health_routes.py
- Create: python-agent/tests/test_health.py

- [ ] **Step 1: Write the failing FastAPI health test**

```python
def test_health_check(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
```

- [ ] **Step 2: Run the test to confirm the service is not initialized yet**

Run: cd python-agent && pytest tests/test_health.py -v
Expected: FAIL because the app does not exist yet

- [ ] **Step 3: Implement the minimal FastAPI app and route**

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
```

- [ ] **Step 4: Run the health test again**

Run: cd python-agent && pytest tests/test_health.py -v
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent
git commit -m "feat: bootstrap python agent health service"
```

### Task 6: Run the phase-1 verification sweep

**Files:**
- Modify: README.md

- [ ] **Step 1: Run the java-core test suite**

Run: cd java-core && .\mvnw test
Expected: PASS

- [ ] **Step 2: Run the python-agent test suite**

Run: cd python-agent && pytest -v
Expected: PASS

- [ ] **Step 3: Run the local smoke path manually**

Run:

```bash
docker compose -f docker/docker-compose.yml up -d
cd java-core && .\mvnw spring-boot:run
cd python-agent && uvicorn app.main:app --reload --port 8000
```

Verify:

- GET /api/health
- POST /api/users
- GET /api/users/{userId}
- GET /health

- [ ] **Step 4: Update README if any command differs from reality**

Run: manual review
Expected: docs match the actual commands used

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: verify phase-1 local runbook"
```

---

## Recommended implementation order

1. Task 1: local runtime and docs
2. Task 2: user_profile migration
3. Task 3-4: java-core health coverage and user API
4. Task 5: python-agent skeleton
5. Task 6: verification sweep

## Notes for execution

- Do not add placement, planning, session, or weekly review code in this phase.
- Keep SQL scope to user_profile only.
- Reuse the existing unified response and exception patterns in java-core.
- Keep python-agent limited to a health endpoint and app skeleton.
- Prefer small focused DTOs and persistence classes over broad placeholder objects.
