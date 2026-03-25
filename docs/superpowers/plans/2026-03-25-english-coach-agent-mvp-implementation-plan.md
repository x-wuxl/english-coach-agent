# English Coach Agent MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an API-first English coach MVP that reliably runs the loop of placement → daily plan → study session → mastery update → weekly review, with Java owning core learning truth and Python owning agent orchestration, memory summaries, and model integrations.

**Architecture:** Use a modular monolith for `java-core` to keep domain rules, persistence, and APIs coherent and testable. Pair it with a separate `python-agent` service that orchestrates sessions, calls model providers, runs reflection/correction loops, and exposes harness replay endpoints. Shared contracts stay explicit through JSON schemas and API DTOs rather than implicit prompt state.

**Tech Stack:** Java 21, Spring Boot 3, Maven, MyBatis-Plus, PostgreSQL, Flyway, JUnit 5, Testcontainers, Python 3.12, FastAPI, Pydantic, httpx, pytest, Docker Compose.

---

## 0. File Structure Map

### Planned top-level layout
- Create: `java-core/`
- Create: `python-agent/`
- Create: `docker/`
- Create: `seeds/`
- Create: `docs/superpowers/plans/`

### Java Core files
- Create: `java-core/pom.xml`
- Create: `java-core/mvnw`
- Create: `java-core/mvnw.cmd`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/EnglishCoachApplication.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/user/UserProfileController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/placement/PlacementController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/DailyPlanController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/StudySessionController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/review/WeeklyReviewController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/user/UserProfileService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/placement/PlacementService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/plan/DailyPlanService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/session/StudySessionService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/scheduler/MemorySchedulerService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/mastery/MasteryStateService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/feedback/FeedbackAnalysisService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/review/WeeklyReviewService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/...`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/...`
- Create: `java-core/src/main/resources/application.yml`
- Create: `java-core/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `java-core/src/main/resources/db/migration/V2__seed_content.sql`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/...`

### Python Agent files
- Create: `python-agent/pyproject.toml`
- Create: `python-agent/app/main.py`
- Create: `python-agent/app/api/session_routes.py`
- Create: `python-agent/app/api/harness_routes.py`
- Create: `python-agent/app/orchestrators/session_orchestrator.py`
- Create: `python-agent/app/agents/coach_response_agent.py`
- Create: `python-agent/app/agents/exercise_generation_agent.py`
- Create: `python-agent/app/agents/reflection_agent.py`
- Create: `python-agent/app/agents/correction_agent.py`
- Create: `python-agent/app/adapters/java_core_client.py`
- Create: `python-agent/app/adapters/provider_router.py`
- Create: `python-agent/app/adapters/providers/base.py`
- Create: `python-agent/app/adapters/providers/mock_provider.py`
- Create: `python-agent/app/adapters/providers/openai_provider.py`
- Create: `python-agent/app/schemas/...`
- Create: `python-agent/app/memory/session_memory_store.py`
- Create: `python-agent/app/harness/replay_runner.py`
- Create: `python-agent/tests/...`

### Shared and infra files
- Create: `docker/docker-compose.yml`
- Create: `seeds/content/learning_items.json`
- Create: `seeds/harness/sample_user_tracks.json`
- Create: `README.md`
- Create: `.env.example`

### Responsibility notes
- `java-core/api/*` only maps HTTP requests/responses, never domain rules.
- `java-core/application/*` owns use cases and coordinates domain services.
- `java-core/domain/*` contains pure rule logic, state transitions, scoring, and value objects.
- `java-core/infrastructure/persistence/*` uses MyBatis-Plus for base CRUD and custom Mapper SQL for complex queries.
- `python-agent/orchestrators/*` wires workflow steps, retries, and fallback paths.
- `python-agent/agents/*` encapsulate one LLM-facing capability each.
- `python-agent/adapters/*` isolate external dependencies like model providers and Java API calls.
- `python-agent/harness/*` replays fixed scenarios and collects metrics.

---

### Task 1: Scaffold repository layout and local runtime

**Files:**
- Create: `java-core/`
- Create: `python-agent/`
- Create: `docker/docker-compose.yml`
- Create: `.env.example`
- Create: `README.md`

- [ ] **Step 1: Write the failing smoke checklist in README**

```md
## Local smoke test
1. Start PostgreSQL with Docker Compose
2. Start java-core on port 8080
3. Start python-agent on port 8000
4. Call health endpoints from both services
5. Verify both respond 200
```

- [ ] **Step 2: Create docker compose for PostgreSQL only**

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

- [ ] **Step 3: Add `.env.example` with both service configs**

```env
JAVA_CORE_BASE_URL=http://localhost:8080
PYTHON_AGENT_BASE_URL=http://localhost:8000
POSTGRES_URL=jdbc:postgresql://localhost:5432/english_coach
POSTGRES_USER=coach
POSTGRES_PASSWORD=coach
DEFAULT_LLM_PROVIDER=mock
```

- [ ] **Step 4: Run Docker Compose and verify PostgreSQL is healthy**

Run: `docker compose -f docker/docker-compose.yml up -d`
Expected: PostgreSQL container starts successfully

- [ ] **Step 5: Commit**

```bash
git add README.md .env.example docker/docker-compose.yml
git commit -m "chore: add local runtime scaffold"
```

### Task 2: Initialize Java Core project skeleton

**Files:**
- Create: `java-core/build.gradle.kts`
- Create: `java-core/settings.gradle.kts`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/EnglishCoachApplication.java`
- Create: `java-core/src/main/resources/application.yml`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/EnglishCoachApplicationTests.java`

- [ ] **Step 1: Write failing Spring Boot context test**

```java
@SpringBootTest
class EnglishCoachApplicationTests {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: Run test to verify project is not initialized yet**

Run: `cd java-core && ./mvnw test`
Expected: FAIL because Maven project files or application classes are not complete yet

- [ ] **Step 3: Align the generated Maven project and minimal Spring Boot app**

```java
@SpringBootApplication
public class EnglishCoachApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnglishCoachApplication.class, args);
    }
}
```

- [ ] **Step 4: Run test to verify context loads**

Run: `cd java-core && ./mvnw test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core
git commit -m "feat: bootstrap java core service"
```

### Task 3: Create base database schema and migrations

**Files:**
- Create: `java-core/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `java-core/src/main/resources/db/migration/V2__seed_content.sql`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/FlywayMigrationTest.java`
- Create: `seeds/content/learning_items.json`

- [ ] **Step 1: Write failing migration smoke test with Testcontainers**

```java
@Test
void flywayMigrationsShouldLoadSchema() {
    assertThat(jdbcTemplate.queryForObject("select count(*) from user_profile", Integer.class)).isEqualTo(0);
}
```

- [ ] **Step 2: Run test to verify tables do not exist yet**

Run: `cd java-core && ./mvnw test --tests FlywayMigrationTest`
Expected: FAIL with relation/table not found

- [ ] **Step 3: Write schema migration for core tables**

Tables to create:
- `user_profile`
- `learning_item`
- `mastery_state`
- `study_session`
- `attempt_log`
- `weekly_review_snapshot`
- `session_reflection`
- `behavior_pattern_summary`

- [ ] **Step 4: Add minimal content seed migration and source JSON**

Seed categories:
- 200~500 high frequency words
- 100~200 phrases
- 100 common expressions
- basic grammar topics

For MVP now, start with 20 words + 10 phrases + 10 expressions + 5 grammar points.

- [ ] **Step 5: Run tests to verify migrations pass**

Run: `cd java-core && ./mvnw test --tests FlywayMigrationTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add java-core/src/main/resources/db seeds/content java-core/src/test
git commit -m "feat: add initial schema and seed content"
```

### Task 4: Implement User Profile API

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/user/UserProfileController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/user/UserProfileService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/user/UserProfile.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileMapper.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/user/UserProfileControllerTest.java`

- [ ] **Step 1: Write failing controller test for create user profile**

```java
@Test
void shouldCreateUserProfile() throws Exception {
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "goal": "general",
                  "dailyMinutes": 20,
                  "studyStartTime": "20:00",
                  "reviewTime": "12:30"
                }
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.goal").value("general"));
}
```

- [ ] **Step 2: Run test to verify endpoint does not exist yet**

Run: `cd java-core && ./mvnw test --tests UserProfileControllerTest`
Expected: FAIL with 404 or missing beans

- [ ] **Step 3: Implement minimal create/get user profile flow**

Controller endpoints:
- `POST /api/users`
- `GET /api/users/{userId}`

- [ ] **Step 4: Run tests to verify create/get passes**

Run: `cd java-core && ./mvnw test --tests UserProfileControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/user java-core/src/main/java/com/wuxl/englishcoach/application/user java-core/src/main/java/com/wuxl/englishcoach/domain/user java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user java-core/src/test/java/com/wuxl/englishcoach/api/user
git commit -m "feat: add user profile api"
```

### Task 5: Implement placement assessment scoring API

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/placement/PlacementController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/placement/PlacementService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/placement/PlacementScorer.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/domain/placement/PlacementScorerTest.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/placement/PlacementControllerTest.java`

- [ ] **Step 1: Write failing domain test for placement scoring**

```java
@Test
void shouldProduceA2PlusProfileForMixedPerformance() {
    PlacementResult result = scorer.score(sampleAnswers());
    assertThat(result.overallLevel()).isEqualTo("A2+");
    assertThat(result.outputLevel()).isEqualTo("A1+");
}
```

- [ ] **Step 2: Run test to verify scorer is missing**

Run: `cd java-core && ./mvnw test --tests PlacementScorerTest`
Expected: FAIL

- [ ] **Step 3: Implement rule-based scorer and placement API**

Endpoint:
- `POST /api/placement/assess`

Response should include:
- overall level
- vocab level
- grammar level
- reading level
- output level
- weaknesses
- suggested daily rhythm

- [ ] **Step 4: Run controller and domain tests**

Run: `cd java-core && ./mvnw test --tests PlacementScorerTest --tests PlacementControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/placement java-core/src/main/java/com/wuxl/englishcoach/application/placement java-core/src/main/java/com/wuxl/englishcoach/domain/placement java-core/src/test/java/com/wuxl/englishcoach/domain/placement java-core/src/test/java/com/wuxl/englishcoach/api/placement
git commit -m "feat: add placement assessment api"
```

### Task 6: Implement daily planner and review priority logic

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/DailyPlanController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/plan/DailyPlanService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/scheduler/MemorySchedulerService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/plan/DailyLoadPolicy.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/scheduler/ReviewPriorityCalculator.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/domain/scheduler/ReviewPriorityCalculatorTest.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/plan/DailyPlanControllerTest.java`

- [ ] **Step 1: Write failing test for review priority formula**

```java
@Test
void shouldRankOverdueWeakOutputItemAboveFreshItem() {
    double overdueScore = calculator.calculate(overdueWeakOutput());
    double freshScore = calculator.calculate(freshStableItem());
    assertThat(overdueScore).isGreaterThan(freshScore);
}
```

- [ ] **Step 2: Run tests to verify scheduler logic is missing**

Run: `cd java-core && ./mvnw test --tests ReviewPriorityCalculatorTest`
Expected: FAIL

- [ ] **Step 3: Implement load policy, scheduler, and daily plan endpoint**

Endpoint:
- `POST /api/plans/daily:generate`

Response should include:
- plan date
- new items
- review items
- output tasks
- rationale per section

- [ ] **Step 4: Run domain and controller tests**

Run: `cd java-core && ./mvnw test --tests ReviewPriorityCalculatorTest --tests DailyPlanControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/plan java-core/src/main/java/com/wuxl/englishcoach/application/plan java-core/src/main/java/com/wuxl/englishcoach/application/scheduler java-core/src/main/java/com/wuxl/englishcoach/domain/plan java-core/src/main/java/com/wuxl/englishcoach/domain/scheduler java-core/src/test/java/com/wuxl/englishcoach/domain/scheduler java-core/src/test/java/com/wuxl/englishcoach/api/plan
git commit -m "feat: add daily planner and review scheduler"
```

### Task 7: Implement study session, attempt logging, and mastery update

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/StudySessionController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/session/StudySessionService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/mastery/MasteryStateService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/feedback/FeedbackAnalysisService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/mastery/MasteryStateMachine.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/domain/mastery/MasteryStateMachineTest.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/session/StudySessionControllerTest.java`

- [ ] **Step 1: Write failing state-machine test for weak_output transition**

```java
@Test
void shouldMoveToWeakOutputWhenRecognitionStableButOutputLow() {
    MasteryState updated = machine.apply(currentState(), weakOutputAttemptSet());
    assertThat(updated.status()).isEqualTo(MasteryStatus.WEAK_OUTPUT);
}
```

- [ ] **Step 2: Run tests to verify mastery transition logic is missing**

Run: `cd java-core && ./mvnw test --tests MasteryStateMachineTest`
Expected: FAIL

- [ ] **Step 3: Implement study session submit endpoint and mastery update flow**

Endpoints:
- `POST /api/sessions/start`
- `POST /api/sessions/{sessionId}/attempts`
- `POST /api/sessions/{sessionId}/complete`

- [ ] **Step 4: Run tests to verify session flow and state transitions**

Run: `cd java-core && ./mvnw test --tests MasteryStateMachineTest --tests StudySessionControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/session java-core/src/main/java/com/wuxl/englishcoach/application/session java-core/src/main/java/com/wuxl/englishcoach/application/mastery java-core/src/main/java/com/wuxl/englishcoach/application/feedback java-core/src/main/java/com/wuxl/englishcoach/domain/mastery java-core/src/test/java/com/wuxl/englishcoach/domain/mastery java-core/src/test/java/com/wuxl/englishcoach/api/session
git commit -m "feat: add study session and mastery update flow"
```

### Task 8: Implement weekly review API

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/review/WeeklyReviewController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/review/WeeklyReviewService.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/review/WeeklyReviewControllerTest.java`

- [ ] **Step 1: Write failing weekly review API test**

```java
@Test
void shouldReturnWeeklySummary() throws Exception {
    mockMvc.perform(get("/api/reviews/weekly").param("userId", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completionRate").exists())
        .andExpect(jsonPath("$.highFrequencyErrorTypes").isArray());
}
```

- [ ] **Step 2: Run test to verify weekly review endpoint is missing**

Run: `cd java-core && ./mvnw test --tests WeeklyReviewControllerTest`
Expected: FAIL

- [ ] **Step 3: Implement weekly review aggregation endpoint**

Fields:
- completion rate
- study minutes
- new/review counts
- frequent error types
- strongest themes
- weakest themes
- next-week suggestion

- [ ] **Step 4: Run tests to verify weekly review passes**

Run: `cd java-core && ./mvnw test --tests WeeklyReviewControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/review java-core/src/main/java/com/wuxl/englishcoach/application/review java-core/src/test/java/com/wuxl/englishcoach/api/review
git commit -m "feat: add weekly review api"
```

### Task 9: Initialize Python Agent service skeleton

**Files:**
- Create: `python-agent/pyproject.toml`
- Create: `python-agent/app/main.py`
- Create: `python-agent/app/api/health_routes.py`
- Test: `python-agent/tests/test_health.py`

- [ ] **Step 1: Write failing FastAPI health test**

```python
def test_health_check(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
```

- [ ] **Step 2: Run test to verify service is missing**

Run: `cd python-agent && pytest tests/test_health.py -v`
Expected: FAIL

- [ ] **Step 3: Implement minimal FastAPI app and health route**

```python
@app.get("/health")
def health():
    return {"status": "ok"}
```

- [ ] **Step 4: Run test to verify health route passes**

Run: `cd python-agent && pytest tests/test_health.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent
git commit -m "feat: bootstrap python agent service"
```

### Task 10: Add Java Core client and session orchestration contract

**Files:**
- Create: `python-agent/app/adapters/java_core_client.py`
- Create: `python-agent/app/schemas/core_contracts.py`
- Create: `python-agent/app/orchestrators/session_orchestrator.py`
- Test: `python-agent/tests/test_java_core_client.py`
- Test: `python-agent/tests/test_session_orchestrator.py`

- [ ] **Step 1: Write failing test for fetching a daily plan from Java Core**

```python
def test_fetch_daily_plan(httpx_mock):
    httpx_mock.add_response(
        method="POST",
        url="http://localhost:8080/api/plans/daily:generate",
        json={"planDate": "2026-03-25", "reviewItems": [], "newItems": []},
    )
    client = JavaCoreClient(base_url="http://localhost:8080")
    result = client.generate_daily_plan(user_id="u1")
    assert result.planDate == "2026-03-25"
```

- [ ] **Step 2: Run tests to verify adapter is missing**

Run: `cd python-agent && pytest tests/test_java_core_client.py tests/test_session_orchestrator.py -v`
Expected: FAIL

- [ ] **Step 3: Implement client DTOs and minimal session orchestration**

Orchestrator flow:
1. fetch user profile
2. fetch/generate daily plan
3. return normalized session context

- [ ] **Step 4: Run tests to verify adapter and orchestrator pass**

Run: `cd python-agent && pytest tests/test_java_core_client.py tests/test_session_orchestrator.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/adapters/java_core_client.py python-agent/app/schemas/core_contracts.py python-agent/app/orchestrators/session_orchestrator.py python-agent/tests/test_java_core_client.py python-agent/tests/test_session_orchestrator.py
git commit -m "feat: add java core client and orchestration contract"
```

### Task 11: Add provider adapter abstraction and mock provider

**Files:**
- Create: `python-agent/app/adapters/providers/base.py`
- Create: `python-agent/app/adapters/providers/mock_provider.py`
- Create: `python-agent/app/adapters/provider_router.py`
- Test: `python-agent/tests/test_provider_router.py`

- [ ] **Step 1: Write failing test for provider selection**

```python
def test_router_returns_mock_provider_by_default():
    router = ProviderRouter(default_provider="mock")
    provider = router.get_chat_provider()
    assert provider.name == "mock"
```

- [ ] **Step 2: Run tests to verify provider layer is missing**

Run: `cd python-agent && pytest tests/test_provider_router.py -v`
Expected: FAIL

- [ ] **Step 3: Implement provider base class, mock provider, and router**

Provider capabilities:
- `generate_text`
- `generate_structured`
- `health_check`

- [ ] **Step 4: Run tests to verify router passes**

Run: `cd python-agent && pytest tests/test_provider_router.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/adapters/providers python-agent/app/adapters/provider_router.py python-agent/tests/test_provider_router.py
git commit -m "feat: add provider abstraction and mock provider"
```

### Task 12: Implement coach response and exercise generation agents

**Files:**
- Create: `python-agent/app/agents/coach_response_agent.py`
- Create: `python-agent/app/agents/exercise_generation_agent.py`
- Create: `python-agent/app/schemas/agent_outputs.py`
- Test: `python-agent/tests/test_coach_response_agent.py`
- Test: `python-agent/tests/test_exercise_generation_agent.py`

- [ ] **Step 1: Write failing tests for coach summary and exercise generation**

```python
def test_coach_response_uses_structured_plan_context(mock_provider):
    result = agent.generate_feedback(session_context, attempt_result)
    assert "今天" in result.message


def test_exercise_generation_returns_schema(mock_provider):
    result = agent.generate_exercises(learning_items)
    assert result.exercises[0].exercise_type == "fill_blank"
```

- [ ] **Step 2: Run tests to verify agent modules are missing**

Run: `cd python-agent && pytest tests/test_coach_response_agent.py tests/test_exercise_generation_agent.py -v`
Expected: FAIL

- [ ] **Step 3: Implement minimal LLM-facing agents backed by mock provider**

Coach output fields:
- message
- tone
- key_observation

Exercise output fields:
- exercise_type
- prompt
- expected_answer
- difficulty

- [ ] **Step 4: Run tests to verify both agents pass**

Run: `cd python-agent && pytest tests/test_coach_response_agent.py tests/test_exercise_generation_agent.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/agents python-agent/app/schemas/agent_outputs.py python-agent/tests/test_coach_response_agent.py python-agent/tests/test_exercise_generation_agent.py
git commit -m "feat: add coach feedback and exercise agents"
```

### Task 13: Implement reflection memory and correction loop

**Files:**
- Create: `python-agent/app/agents/reflection_agent.py`
- Create: `python-agent/app/agents/correction_agent.py`
- Create: `python-agent/app/memory/session_memory_store.py`
- Test: `python-agent/tests/test_reflection_agent.py`
- Test: `python-agent/tests/test_correction_agent.py`
- Test: `python-agent/tests/test_session_memory_store.py`

- [ ] **Step 1: Write failing tests for reflection output and correction fallback**

```python
def test_reflection_agent_returns_structured_summary(mock_provider):
    result = agent.reflect(session_trace)
    assert result.detected_patterns
    assert result.recommended_adjustment in {"reduce_new_items", "keep"}


def test_correction_agent_falls_back_to_rule_result_on_conflict():
    corrected = agent.resolve(rule_result, llm_result)
    assert corrected.final_source == "rule"
```

- [ ] **Step 2: Run tests to verify modules are missing**

Run: `cd python-agent && pytest tests/test_reflection_agent.py tests/test_correction_agent.py tests/test_session_memory_store.py -v`
Expected: FAIL

- [ ] **Step 3: Implement structured reflection, correction logic, and memory persistence adapter**

Memory record should include:
- session id
- patterns
- fatigue signal
- adjustment hint
- confidence

- [ ] **Step 4: Run tests to verify reflection/correction passes**

Run: `cd python-agent && pytest tests/test_reflection_agent.py tests/test_correction_agent.py tests/test_session_memory_store.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/agents/reflection_agent.py python-agent/app/agents/correction_agent.py python-agent/app/memory/session_memory_store.py python-agent/tests/test_reflection_agent.py python-agent/tests/test_correction_agent.py python-agent/tests/test_session_memory_store.py
git commit -m "feat: add reflection memory and correction loop"
```

### Task 14: Expose agent session API that wraps the full learning flow

**Files:**
- Create: `python-agent/app/api/session_routes.py`
- Modify: `python-agent/app/orchestrators/session_orchestrator.py`
- Test: `python-agent/tests/test_session_routes.py`

- [ ] **Step 1: Write failing API test for session preview/start flow**

```python
def test_start_session_returns_plan_and_coach_intro(client, httpx_mock):
    response = client.post("/api/agent/sessions/start", json={"userId": "u1"})
    assert response.status_code == 200
    body = response.json()
    assert "plan" in body
    assert "coachMessage" in body
```

- [ ] **Step 2: Run tests to verify session route is missing**

Run: `cd python-agent && pytest tests/test_session_routes.py -v`
Expected: FAIL

- [ ] **Step 3: Implement route that orchestrates Java plan + coach intro + optional exercise generation**

Endpoint:
- `POST /api/agent/sessions/start`

- [ ] **Step 4: Run tests to verify session route passes**

Run: `cd python-agent && pytest tests/test_session_routes.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/api/session_routes.py python-agent/app/orchestrators/session_orchestrator.py python-agent/tests/test_session_routes.py
git commit -m "feat: expose agent session api"
```

### Task 15: Implement harness replay runner and API

**Files:**
- Create: `python-agent/app/harness/replay_runner.py`
- Create: `python-agent/app/api/harness_routes.py`
- Create: `seeds/harness/sample_user_tracks.json`
- Test: `python-agent/tests/test_replay_runner.py`
- Test: `python-agent/tests/test_harness_routes.py`

- [ ] **Step 1: Write failing test for replaying a fixed user scenario**

```python
def test_replay_runner_returns_metrics(httpx_mock):
    result = runner.run(track=sample_track())
    assert result.plan_relevance_score >= 0
    assert result.correction_success_rate >= 0
```

- [ ] **Step 2: Run tests to verify harness runner is missing**

Run: `cd python-agent && pytest tests/test_replay_runner.py tests/test_harness_routes.py -v`
Expected: FAIL

- [ ] **Step 3: Implement replay runner and harness route**

Endpoint:
- `POST /api/harness/replay`

Metrics:
- plan relevance score
- correction success rate
- fallback success rate
- average latency ms

- [ ] **Step 4: Run tests to verify harness passes**

Run: `cd python-agent && pytest tests/test_replay_runner.py tests/test_harness_routes.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/harness python-agent/app/api/harness_routes.py seeds/harness python-agent/tests/test_replay_runner.py python-agent/tests/test_harness_routes.py
git commit -m "feat: add harness replay api"
```

### Task 16: Add end-to-end contract verification between Java Core and Python Agent

**Files:**
- Create: `python-agent/tests/test_core_contract_e2e.py`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/contracts/AgentContractFixtureTest.java`
- Modify: `README.md`

- [ ] **Step 1: Write failing contract test using fixture responses**

```python
def test_agent_can_consume_java_plan_contract(sample_plan_response):
    payload = SessionContext.model_validate(sample_plan_response)
    assert payload.planDate
```

- [ ] **Step 2: Run tests to verify contract fixture is incomplete**

Run: `cd python-agent && pytest tests/test_core_contract_e2e.py -v`
Expected: FAIL

- [ ] **Step 3: Add fixture generation notes and contract assertions on both sides**

Contract targets:
- user profile DTO
- placement result DTO
- daily plan DTO
- session attempt DTO
- weekly review DTO

- [ ] **Step 4: Run cross-service tests**

Run: `cd java-core && ./mvnw test`
Run: `cd python-agent && pytest -v`
Expected: PASS on both sides

- [ ] **Step 5: Commit**

```bash
git add README.md java-core/src/test/java/com/wuxl/englishcoach/contracts python-agent/tests/test_core_contract_e2e.py
git commit -m "test: add cross-service contract verification"
```

### Task 17: Add minimal provider integration beyond mock

**Files:**
- Create: `python-agent/app/adapters/providers/openai_provider.py`
- Modify: `python-agent/app/adapters/provider_router.py`
- Test: `python-agent/tests/test_openai_provider.py`

- [ ] **Step 1: Write failing adapter test that validates request mapping and secret-free logging**

```python
def test_openai_provider_masks_sensitive_config(caplog):
    provider = OpenAIProvider(api_key="sk-test-secret")
    provider.health_check()
    assert "sk-test-secret" not in caplog.text
```

- [ ] **Step 2: Run tests to verify provider is missing**

Run: `cd python-agent && pytest tests/test_openai_provider.py -v`
Expected: FAIL

- [ ] **Step 3: Implement provider adapter with safe logging and timeout handling**

Requirements:
- never log raw API keys
- configurable model name
- timeout and retry support

- [ ] **Step 4: Run provider tests**

Run: `cd python-agent && pytest tests/test_openai_provider.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add python-agent/app/adapters/providers/openai_provider.py python-agent/app/adapters/provider_router.py python-agent/tests/test_openai_provider.py
git commit -m "feat: add openai provider adapter"
```

### Task 18: Write end-to-end runbook and MVP verification checklist

**Files:**
- Modify: `README.md`
- Create: `docs/superpowers/plans/verification-checklist.md`

- [ ] **Step 1: Write failing manual checklist by describing exact MVP demo path**

```md
1. Create user profile
2. Submit placement answers
3. Generate daily plan
4. Start agent session
5. Submit attempts
6. Complete session
7. Generate weekly review
8. Replay harness scenario
```

- [ ] **Step 2: Verify at least one missing command or path exists in docs**

Run: manual review
Expected: find missing details before finalizing docs

- [ ] **Step 3: Fill in exact curl commands and expected outputs**

Include:
- Java endpoints
- Python endpoints
- docker startup
- smoke test commands

- [ ] **Step 4: Execute the checklist manually and update docs with any fixes**

Run: full local walkthrough
Expected: all commands work as documented

- [ ] **Step 5: Commit**

```bash
git add README.md docs/superpowers/plans/verification-checklist.md
git commit -m "docs: add mvp verification checklist"
```

---

## Recommended implementation order

1. Task 1-3: local runtime + Java skeleton + schema
2. Task 4-8: Java MVP domain loop
3. Task 9-14: Python orchestration + provider abstraction + reflection/correction
4. Task 15-17: harness + provider integration + contract checks
5. Task 18: runbook and MVP validation

## Notes for execution

- Keep Java as the sole owner of mastery-state truth.
- Keep Python focused on orchestration and AI-facing concerns.
- Do not add vector memory, voice, or a full web frontend in this MVP.
- Prefer deterministic fixtures and mock providers early; only add real provider integration after the full loop works locally.
- Every API DTO used across services must get a contract test.
- Reflection output must stay structured and advisory, never direct fact replacement.
