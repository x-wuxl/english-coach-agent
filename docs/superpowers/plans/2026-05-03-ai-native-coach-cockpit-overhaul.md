# AI Native Coach Cockpit Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current module-first demo with a Today Coach cockpit that supports free conversation, lightweight saved notes, priority learner memory, and inline mini drills.

**Architecture:** This is a product and architecture overhaul, not a UI polish pass. Keep `java-core` as the source of truth for sessions, memory, drill priority, and persistence; keep `python-agent` responsible for structured language analysis and coach phrasing; replace the static demo's primary experience with `Today Coach` while keeping old modules as secondary views during migration.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, Flyway, H2/MockMvc tests, Python 3.11, FastAPI, Pydantic v2, pytest, static HTML/CSS/JS in `python-agent/app/static/index.html`.

---

## Reference Documents

- Product spec: `docs/superpowers/specs/2026-05-03-ai-native-coach-cockpit-design.md`
- Current product/architecture spec: `docs/superpowers/specs/2026-05-03-ai-native-coach-cockpit-design.md`
- Current static UI: `python-agent/app/static/index.html`
- Existing Python coach feedback: `python-agent/app/agents/coach_agent.py`
- Existing Java LLM client: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java`

## Scope Decision

This plan intentionally treats the change as a large overhaul. The goal is not to incrementally decorate `Dashboard` and `Study`; the goal is to introduce a new `Today Coach` product loop and move old flows behind secondary navigation.

The overhaul is split into testable commits:

```text
1. Schema and persistence for Coach/Memory
2. Domain rules for Memory priority and drill suggestion
3. Python structured coach-turn analysis
4. Java PythonAgentClient typed contract
5. Java Coach API orchestration
6. Today Coach cockpit UI
7. First Coaching Session
8. Coach Review / Progress repositioning
```

## File Map

### Java Core: New Files

- `java-core/src/main/resources/db/migration/V21__create_coach_memory_tables.sql`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/memory/ErrorPatternDO.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/memory/ExpressionGapDO.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/memory/ErrorPatternMapper.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/memory/ExpressionGapMapper.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/coach/CoachSessionDO.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/coach/CoachTurnDO.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/coach/CoachSessionMapper.java`
- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/coach/CoachTurnMapper.java`
- `java-core/src/main/java/com/wuxl/englishcoach/domain/memory/MemoryPriorityPolicy.java`
- `java-core/src/main/java/com/wuxl/englishcoach/domain/memory/DrillSuggestionPolicy.java`
- `java-core/src/main/java/com/wuxl/englishcoach/application/memory/MemoryService.java`
- `java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java`
- `java-core/src/main/java/com/wuxl/englishcoach/api/memory/MemoryController.java`
- `java-core/src/main/java/com/wuxl/englishcoach/api/coach/CoachController.java`
- DTO packages under `java-core/src/main/java/com/wuxl/englishcoach/api/memory/dto/` and `java-core/src/main/java/com/wuxl/englishcoach/api/coach/dto/`.

### Java Core: Modified Files

- `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java`
- `java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/SchemaMigrationTest.java`
- `java-core/src/main/java/com/wuxl/englishcoach/common/enums/ErrorCodeEnum.java` only if new error codes are needed.

### Python Agent: New Files

- `python-agent/tests/test_coach_turn_agent.py`
- `python-agent/tests/test_coach_turn_routes.py`
- `python-agent/tests/test_first_coaching_agent.py`

### Python Agent: Modified Files

- `python-agent/app/api/dto.py`
- `python-agent/app/agents/coach_agent.py`
- `python-agent/app/api/coach_routes.py`
- `python-agent/tests/test_coach_agent.py`

### Frontend: Modified File

- `python-agent/app/static/index.html`

## Cross-Cutting Rules

- Use TDD for every behavior change: write the failing test first, run it, then implement.
- Commit after each task. Do not batch multiple tasks into one commit.
- Do not touch unrelated vocabulary migration changes already present in the working tree.
- Keep LLM outputs structured and validated. Do not let LLM directly own memory state or drill scheduling.
- For Java integration tests, follow the existing `@SpringBootTest` + `MockMvc` + H2 pattern.
- For Python tests, patch `llm_service` as existing tests do.

---

### Task 1: Coach and Memory Schema

**Files:**
- Create: `java-core/src/main/resources/db/migration/V21__create_coach_memory_tables.sql`
- Create: persistence DO/Mapper classes under `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/memory/`
- Create: persistence DO/Mapper classes under `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/coach/`
- Modify: `java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/SchemaMigrationTest.java`

- [ ] **Step 1: Write failing schema tests**

Add tests to `SchemaMigrationTest`:

```java
@Test
void coachMemoryTablesShouldExist() {
    String[] tables = {"error_pattern", "expression_gap", "coach_session", "coach_turn"};

    for (String table : tables) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where upper(table_name) = upper(?)",
                Integer.class, table
        );
        assertThat(count).as("Table %s should exist", table).isEqualTo(1);
    }
}

@Test
void errorPatternShouldHaveUserPatternUniqueConstraint() {
    Integer count = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.indexes where upper(table_name) = 'ERROR_PATTERN' and upper(index_name) like '%USER_PATTERN%'",
            Integer.class
    );
    assertThat(count).isEqualTo(1);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `java-core`:

```bash
mvn test -Dtest=SchemaMigrationTest
```

Expected: FAIL because new tables do not exist.

- [ ] **Step 3: Add Flyway migration**

Create `V21__create_coach_memory_tables.sql` with these tables:

```sql
create table error_pattern (
    id bigint generated by default as identity primary key,
    user_id bigint not null,
    pattern_key varchar(128) not null,
    label varchar(255) not null,
    description_zh varchar(2048),
    user_examples text not null default '[]',
    better_examples text not null default '[]',
    seen_count int not null default 1,
    severity varchar(32) not null default 'MEDIUM',
    status varchar(32) not null default 'ACTIVE',
    last_seen_at timestamp not null default current_timestamp,
    next_drill_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_error_pattern_user_pattern unique (user_id, pattern_key)
);

create index idx_error_pattern_user_status on error_pattern(user_id, status);
create index idx_error_pattern_user_next_drill on error_pattern(user_id, next_drill_at);

create table expression_gap (
    id bigint generated by default as identity primary key,
    user_id bigint not null,
    gap_key varchar(128) not null,
    zh_intent varchar(512) not null,
    natural_expressions text not null default '[]',
    user_attempts text not null default '[]',
    context varchar(128),
    seen_count int not null default 1,
    status varchar(32) not null default 'ACTIVE',
    last_seen_at timestamp not null default current_timestamp,
    next_drill_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_expression_gap_user_gap unique (user_id, gap_key)
);

create index idx_expression_gap_user_status on expression_gap(user_id, status);
create index idx_expression_gap_user_next_drill on expression_gap(user_id, next_drill_at);

create table coach_session (
    id bigint generated by default as identity primary key,
    session_code varchar(64) not null,
    user_id bigint not null,
    session_type varchar(32) not null,
    status varchar(32) not null default 'STARTED',
    summary text,
    detected_level_range varchar(32),
    started_at timestamp not null default current_timestamp,
    ended_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_coach_session_code unique (session_code)
);

create index idx_coach_session_user_started on coach_session(user_id, started_at);

create table coach_turn (
    id bigint generated by default as identity primary key,
    coach_session_id bigint not null,
    mode varchar(32) not null,
    user_message text,
    coach_message text,
    detected_notes text not null default '[]',
    created_at timestamp not null default current_timestamp
);

create index idx_coach_turn_session_created on coach_turn(coach_session_id, created_at);
```

- [ ] **Step 4: Add DO and Mapper classes**

Follow existing Lombok/MyBatis style. Example:

```java
@Getter
@Setter
@TableName("error_pattern")
public class ErrorPatternDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String patternKey;
    private String label;
    private String descriptionZh;
    private String userExamples;
    private String betterExamples;
    private Integer seenCount;
    private String severity;
    private String status;
    private LocalDateTime lastSeenAt;
    private LocalDateTime nextDrillAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Mapper pattern:

```java
public interface ErrorPatternMapper extends BaseMapper<ErrorPatternDO> {
}
```

- [ ] **Step 5: Run schema test**

Run:

```bash
mvn test -Dtest=SchemaMigrationTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add java-core/src/main/resources/db/migration/V21__create_coach_memory_tables.sql \
  java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/memory \
  java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/coach \
  java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/SchemaMigrationTest.java
git commit -m "feat: add coach memory persistence schema"
```

---

### Task 2: Memory Priority and Drill Rules

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/memory/MemoryPriorityPolicy.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/memory/DrillSuggestionPolicy.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/domain/memory/MemoryPriorityPolicyTest.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/domain/memory/DrillSuggestionPolicyTest.java`

- [ ] **Step 1: Write failing priority tests**

```java
class MemoryPriorityPolicyTest {
    private final MemoryPriorityPolicy policy = new MemoryPriorityPolicy();

    @Test
    void repeatedDueItemShouldRankAboveNewItem() {
        var repeated = new MemoryPriorityPolicy.MemorySnapshot(
                "ERROR_PATTERN", 1L, "need to + verb", 2, "MEDIUM", "ACTIVE",
                LocalDateTime.now().minusDays(1));
        var fresh = new MemoryPriorityPolicy.MemorySnapshot(
                "EXPRESSION_GAP", 2L, "我来不及了", 1, "MEDIUM", "ACTIVE",
                LocalDateTime.now().plusDays(3));

        assertThat(policy.score(repeated)).isGreaterThan(policy.score(fresh));
    }
}
```

- [ ] **Step 2: Write failing drill suggestion tests**

```java
class DrillSuggestionPolicyTest {
    private final DrillSuggestionPolicy policy = new DrillSuggestionPolicy();

    @Test
    void shouldSuggestDrillWhenSameErrorPatternAppearsTwice() {
        assertThat(policy.shouldSuggest("ERROR_PATTERN", 2, "ACTIVE")).isTrue();
    }

    @Test
    void shouldNotSuggestDrillForFirstOccurrence() {
        assertThat(policy.shouldSuggest("ERROR_PATTERN", 1, "ACTIVE")).isFalse();
    }
}
```

- [ ] **Step 3: Run tests to verify failure**

```bash
mvn test -Dtest=MemoryPriorityPolicyTest,DrillSuggestionPolicyTest
```

Expected: FAIL because policy classes do not exist.

- [ ] **Step 4: Implement deterministic rules**

Use simple scoring: repeated count, due date, severity, and status.

```java
public double score(MemorySnapshot snapshot) {
    double score = Math.min(snapshot.seenCount(), 5) * 10;
    if (snapshot.nextDrillAt() != null && !snapshot.nextDrillAt().isAfter(LocalDateTime.now())) score += 50;
    if ("HIGH".equals(snapshot.severity())) score += 20;
    if ("IMPROVING".equals(snapshot.status())) score -= 10;
    return score;
}
```

```java
public boolean shouldSuggest(String memoryType, int seenCount, String status) {
    return "ACTIVE".equals(status) && seenCount >= 2;
}
```

- [ ] **Step 5: Run policy tests**

```bash
mvn test -Dtest=MemoryPriorityPolicyTest,DrillSuggestionPolicyTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/domain/memory \
  java-core/src/test/java/com/wuxl/englishcoach/domain/memory
git commit -m "feat: add memory priority and drill rules"
```

---

### Task 3: Python Structured Coach Turn Analysis

**Files:**
- Modify: `python-agent/app/api/dto.py`
- Modify: `python-agent/app/agents/coach_agent.py`
- Modify: `python-agent/app/api/coach_routes.py`
- Create: `python-agent/tests/test_coach_turn_agent.py`
- Create: `python-agent/tests/test_coach_turn_routes.py`

- [ ] **Step 1: Write failing agent tests**

Create `test_coach_turn_agent.py`:

```python
from unittest.mock import patch

from app.api.dto import CoachTurnAnalyzeRequest
from app.agents.coach_agent import analyze_turn


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_returns_saved_error_pattern(mock_llm):
    mock_llm.structured.return_value = {
        "coach_reply": "What part of the demo feels hardest?",
        "saved_notes": [{
            "type": "ERROR_PATTERN",
            "key": "missing_infinitive_to",
            "label": "need to + verb",
            "description_zh": "need 后面接动词时要加 to。",
            "user_text": "I need prepare the demo.",
            "better_text": "I need to prepare the demo.",
            "severity": "MEDIUM",
            "confidence": 0.9,
        }],
        "expression_gaps": [],
        "fix_response": None,
    }

    resp = analyze_turn(CoachTurnAnalyzeRequest(
        mode="CHAT",
        message="I need prepare the demo.",
        recent_memory=[],
    ))

    assert resp.coach_reply == "What part of the demo feels hardest?"
    assert resp.saved_notes[0].key == "missing_infinitive_to"
```

- [ ] **Step 2: Write failing route test**

Create `test_coach_turn_routes.py`:

```python
from fastapi.testclient import TestClient
from unittest.mock import patch

from app.main import app


client = TestClient(app)


@patch("app.agents.coach_agent.llm_service")
def test_analyze_turn_route(mock_llm):
    mock_llm.structured.return_value = {
        "coach_reply": "Tell me more.",
        "saved_notes": [],
        "expression_gaps": [],
        "fix_response": None,
    }

    resp = client.post("/api/coach/turn/analyze", json={"mode": "CHAT", "message": "Hi", "recent_memory": []})

    assert resp.status_code == 200
    assert resp.json()["coach_reply"] == "Tell me more."
```

- [ ] **Step 3: Run tests to verify failure**

```bash
cd python-agent
python -m pytest tests/test_coach_turn_agent.py tests/test_coach_turn_routes.py -q
```

Expected: FAIL because DTOs/routes/functions do not exist.

- [ ] **Step 4: Add DTOs**

Add to `python-agent/app/api/dto.py`:

```python
class SavedNote(BaseModel):
    type: str
    key: str
    label: str
    description_zh: str | None = None
    user_text: str
    better_text: str | None = None
    severity: str = "MEDIUM"
    confidence: float = 0.0


class ExpressionGapNote(BaseModel):
    key: str
    zh_intent: str
    natural_expressions: list[str] = []
    user_attempt: str | None = None
    context: str | None = None
    confidence: float = 0.0


class FixResponse(BaseModel):
    meaning_check: str
    better_english: str
    what_changed: list[str] = []
    memory_update: str | None = None
    try_again_prompt: str | None = None


class CoachTurnAnalyzeRequest(BaseModel):
    mode: str
    message: str
    recent_memory: list[dict] = []


class CoachTurnAnalyzeResponse(BaseModel):
    coach_reply: str
    saved_notes: list[SavedNote] = []
    expression_gaps: list[ExpressionGapNote] = []
    fix_response: FixResponse | None = None
```

- [ ] **Step 5: Implement `analyze_turn()`**

Keep existing `generate_feedback()` intact. Add a new structured prompt and safe fallback:

```python
def analyze_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    result = llm_service.structured(
        messages=[...],
        response_model=CoachTurnAnalyzeResponse,
        model=settings.coach_model,
        temperature=0.4,
        max_tokens=4000,
    )
    if result is None:
        return CoachTurnAnalyzeResponse(coach_reply="Tell me more about that.")
    if isinstance(result, CoachTurnAnalyzeResponse):
        return result
    return CoachTurnAnalyzeResponse(**result)
```

- [ ] **Step 6: Add route**

In `coach_routes.py`:

```python
@router.post("/turn/analyze", response_model=CoachTurnAnalyzeResponse)
def analyze_coach_turn(req: CoachTurnAnalyzeRequest) -> CoachTurnAnalyzeResponse:
    return analyze_turn(req)
```

- [ ] **Step 7: Run Python tests**

```bash
cd python-agent
python -m pytest tests/test_coach_agent.py tests/test_coach_turn_agent.py tests/test_coach_turn_routes.py -q
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add python-agent/app/api/dto.py python-agent/app/agents/coach_agent.py python-agent/app/api/coach_routes.py \
  python-agent/tests/test_coach_agent.py python-agent/tests/test_coach_turn_agent.py python-agent/tests/test_coach_turn_routes.py
git commit -m "feat: add structured coach turn analysis"
```

---

### Task 4: Java Typed Python Agent Client

**Files:**
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java`
- Create DTO records under `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClientTest.java`

- [ ] **Step 1: Write failing client test**

Use a test server or Spring's REST client testing support to verify that `analyzeCoachTurn()` posts to `/api/coach/turn/analyze` and maps `coach_reply` plus `saved_notes` into typed Java records.

- [ ] **Step 2: Run test to verify failure**

```bash
cd java-core
mvn test -Dtest=PythonAgentClientTest
```

Expected: FAIL because typed client methods and DTOs do not exist.

- [ ] **Step 3: Add DTO records**

Create records for request, response, and saved notes. Use `@JsonProperty` for snake_case fields returned by Python.

```java
public record CoachTurnAnalysisRequest(String mode, String message, List<Map<String, Object>> recentMemory) {}

public record SavedNoteDto(
        String type,
        String key,
        String label,
        @JsonProperty("description_zh") String descriptionZh,
        @JsonProperty("user_text") String userText,
        @JsonProperty("better_text") String betterText,
        String severity,
        Double confidence
) {}

public record CoachTurnAnalysisResponse(
        @JsonProperty("coach_reply") String coachReply,
        @JsonProperty("saved_notes") List<SavedNoteDto> savedNotes,
        @JsonProperty("expression_gaps") List<Map<String, Object>> expressionGaps,
        @JsonProperty("fix_response") Map<String, Object> fixResponse
) {}
```

- [ ] **Step 4: Add client method**

In `PythonAgentClient`:

```java
public CoachTurnAnalysisResponse analyzeCoachTurn(CoachTurnAnalysisRequest request) {
    if (!enabled) return null;
    try {
        return restClient.post()
                .uri("/api/coach/turn/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CoachTurnAnalysisResponse.class);
    } catch (Exception e) {
        log.warn("Failed to analyze coach turn via python-agent: {}", e.getMessage());
        return null;
    }
}
```

- [ ] **Step 5: Run client tests**

```bash
cd java-core
mvn test -Dtest=PythonAgentClientTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm \
  java-core/src/test/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClientTest.java
git commit -m "feat: add typed coach analysis client"
```

---

### Task 5: Java Memory Service and API

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/memory/MemoryService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/memory/MemoryController.java`
- Create DTOs under `java-core/src/main/java/com/wuxl/englishcoach/api/memory/dto/`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/memory/MemoryControllerTest.java`

- [ ] **Step 1: Write failing API tests**

Create `MemoryControllerTest` using existing `MockMvc` style. It should insert an `error_pattern` row and assert:

```text
GET /api/memory/priority?userId={id}
-> data.items[0].label == "need to + verb"
-> data.items[0].recommendedAction == "START_DRILL"
```

- [ ] **Step 2: Run test to verify failure**

```bash
cd java-core
mvn test -Dtest=MemoryControllerTest
```

Expected: FAIL because endpoint does not exist.

- [ ] **Step 3: Implement Memory DTOs**

```java
public record PriorityMemoryItemResponse(
        String memoryType,
        Long memoryId,
        String label,
        String sourceText,
        String betterText,
        Integer seenCount,
        String status,
        String recommendedAction,
        LocalDateTime nextDrillAt,
        double priorityScore
) {}

public record PriorityMemoryResponse(List<PriorityMemoryItemResponse> items) {}
```

- [ ] **Step 4: Implement MemoryService**

Responsibilities:

- Validate user exists.
- Query active `error_pattern` and `expression_gap` rows for user.
- Convert rows to priority snapshots.
- Sort by descending score.
- Return top 5.

- [ ] **Step 5: Implement MemoryController**

```java
@RestController
@RequestMapping("/api/memory")
public class MemoryController {
    @GetMapping("/priority")
    public BaseResponse<PriorityMemoryResponse> priority(@RequestParam Long userId) {
        return BaseResponse.success(memoryService.getPriorityMemory(userId));
    }
}
```

- [ ] **Step 6: Run memory API tests**

```bash
cd java-core
mvn test -Dtest=MemoryControllerTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/application/memory \
  java-core/src/main/java/com/wuxl/englishcoach/api/memory \
  java-core/src/test/java/com/wuxl/englishcoach/api/memory/MemoryControllerTest.java
git commit -m "feat: expose priority learner memory"
```

---

### Task 6: Java Coach Session API Orchestration

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/coach/CoachController.java`
- Create DTOs under `java-core/src/main/java/com/wuxl/englishcoach/api/coach/dto/`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/memory/MemoryService.java`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/coach/CoachControllerTest.java`

- [ ] **Step 1: Write failing Coach API tests**

Test session creation:

```java
@Test
void shouldStartTodayCoachSession() throws Exception {
    Long userId = createTestUser();

    mockMvc.perform(post("/api/coach/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userId": %d, "sessionType": "TODAY_COACH"}
                            """.formatted(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionType").value("TODAY_COACH"))
            .andExpect(jsonPath("$.data.sessionCode").isNotEmpty());
}
```

Test turn submission with python-agent disabled fallback:

```java
@Test
void shouldSaveChatTurnAndReturnPriorityMemory() throws Exception {
    Long userId = createTestUser();
    Long sessionId = startCoachSession(userId);

    mockMvc.perform(post("/api/coach/sessions/" + sessionId + "/turns")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"mode": "CHAT", "message": "I need prepare the demo."}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.coachReply").isNotEmpty())
            .andExpect(jsonPath("$.data.priorityMemory.items").isArray());
}
```

- [ ] **Step 2: Run tests to verify failure**

```bash
cd java-core
mvn test -Dtest=CoachControllerTest
```

Expected: FAIL because API does not exist.

- [ ] **Step 3: Add Coach DTOs**

Records:

```java
public record StartCoachSessionRequest(Long userId, String sessionType) {}
public record CoachSessionResponse(Long id, String sessionCode, Long userId, String sessionType, String status) {}
public record SubmitCoachTurnRequest(String mode, String message) {}
public record SavedNoteResponse(String type, String key, String label, String userText, String betterText) {}
public record DrillSuggestionResponse(String memoryType, Long memoryId, String prompt) {}
public record CoachTurnResponse(
        String coachReply,
        List<SavedNoteResponse> savedNotes,
        PriorityMemoryResponse priorityMemory,
        DrillSuggestionResponse drillSuggestion
) {}
```

- [ ] **Step 4: Implement CoachSessionService**

Responsibilities:

- Start `coach_session` after validating user.
- Submit a turn.
- Call `PythonAgentClient.analyzeCoachTurn()`.
- Fallback to a simple coach reply when python-agent is disabled/unavailable.
- Store `coach_turn` with detected notes JSON.
- Merge saved notes into `ErrorPattern` / `ExpressionGap` through `MemoryService`.
- Return priority memory and drill suggestion.

Merge rule for error patterns:

```text
if (user_id, pattern_key) exists:
  increment seen_count
  append user_text and better_text to JSON arrays if not already present
  update severity, last_seen_at, updated_at
else:
  insert with seen_count = 1
```

Drill suggestion rule:

```text
Suggest only when the just-merged memory item reaches seen_count >= 2 and status ACTIVE.
```

- [ ] **Step 5: Implement CoachController**

Endpoints:

```text
POST /api/coach/sessions
POST /api/coach/sessions/{sessionId}/turns
```

- [ ] **Step 6: Run Coach API tests**

```bash
cd java-core
mvn test -Dtest=CoachControllerTest,MemoryControllerTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/application/coach \
  java-core/src/main/java/com/wuxl/englishcoach/api/coach \
  java-core/src/main/java/com/wuxl/englishcoach/application/memory \
  java-core/src/test/java/com/wuxl/englishcoach/api/coach/CoachControllerTest.java
git commit -m "feat: add coach session orchestration"
```

---

### Task 7: Today Coach Cockpit UI Overhaul

**Files:**
- Modify: `python-agent/app/static/index.html`

- [ ] **Step 1: Create a manual UI test checklist before editing**

Use this checklist while working:

```text
- Default page is Today Coach.
- User can create/load user from Today Coach.
- Start Coach creates a coach session.
- Chat mode submits to Java /api/coach/sessions/{id}/turns.
- Saved notes render in chat.
- Priority Memory renders on the right.
- Drill suggestion renders with Start / Not now.
- Legacy Dashboard/Study/Placement/Mastery/Weekly remain reachable as secondary views.
```

- [ ] **Step 2: Refactor navigation**

Change nav order to:

```text
Today Coach | Memory | Progress | Legacy
```

Inside `Legacy`, keep old Dashboard, Study, Placement, Mastery, Weekly Review controls initially. Do not delete old functions yet.

- [ ] **Step 3: Add Today Coach markup**

Add a new `page-coach` structure:

```html
<div id="page-coach">
  <section class="coach-shell">
    <header class="coach-brief">...</header>
    <main class="coach-stream" id="coach-stream"></main>
    <aside class="memory-panel" id="priority-memory"></aside>
    <footer class="coach-input">...</footer>
  </section>
</div>
```

- [ ] **Step 4: Add JS state**

```javascript
let coachSessionId = null;
let coachMode = 'CHAT';
let coachMessages = [];
let priorityMemory = [];
```

- [ ] **Step 5: Implement API functions**

```javascript
async function startCoachSession() { ... }
async function submitCoachTurn() { ... }
async function loadPriorityMemory(userId) { ... }
function renderCoachStream() { ... }
function renderPriorityMemory(items) { ... }
function renderSavedNote(note) { ... }
function renderDrillSuggestion(suggestion) { ... }
```

- [ ] **Step 6: Implement mode selector behavior**

Mode selector updates `coachMode` and placeholder text:

```text
CHAT  -> Say anything in English...
FIX   -> Paste one English sentence to diagnose...
DRILL -> Choose a memory item or type what you want to practice...
```

- [ ] **Step 7: Manual browser verification**

Start services in separate terminals:

```bash
cd java-core && mvn spring-boot:run
cd python-agent && uvicorn app.main:app --reload --port 8000
```

Open:

```text
http://localhost:8000
```

Expected: Today Coach is the default screen and can complete at least one chat turn using fallback or real python-agent.

- [ ] **Step 8: Commit**

```bash
git add python-agent/app/static/index.html
git commit -m "feat: introduce today coach cockpit UI"
```

---

### Task 8: First Coaching Session

**Files:**
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/coach/CoachController.java`
- Modify DTOs under `java-core/src/main/java/com/wuxl/englishcoach/api/coach/dto/`
- Modify: `python-agent/app/agents/coach_agent.py`
- Modify: `python-agent/app/api/dto.py`
- Modify: `python-agent/app/api/coach_routes.py`
- Modify: `python-agent/app/static/index.html`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/coach/FirstCoachingSessionControllerTest.java`
- Test: `python-agent/tests/test_first_coaching_agent.py`

- [ ] **Step 1: Write failing Java first-session test**

```java
@Test
void shouldCompleteFirstCoachingSessionAndReturnLevelRange() throws Exception {
    Long userId = createTestUser();

    mockMvc.perform(post("/api/coach/sessions:first")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "userId": %d,
                              "goal": "GENERAL",
                              "dailyMinutes": 10,
                              "samples": ["I want improve my English because my work need it."]
                            }
                            """.formatted(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.detectedLevelRange").isNotEmpty())
            .andExpect(jsonPath("$.data.initialMemory.items").isArray());
}
```

- [ ] **Step 2: Write failing Python first-session test**

Test that a sample sentence returns weak level range and initial notes.

- [ ] **Step 3: Implement first-session DTOs and routes**

Java endpoint:

```text
POST /api/coach/sessions:first
```

Python endpoint:

```text
POST /api/coach/first/analyze
```

- [ ] **Step 4: Update UI first-run state**

If user has no coach session/memory, show First Coaching Session panel above or instead of chat:

```text
Goal
Daily minutes
2-3 sample prompts
Start my coach plan
```

- [ ] **Step 5: Run focused tests**

```bash
cd java-core && mvn test -Dtest=FirstCoachingSessionControllerTest,CoachControllerTest
cd python-agent && python -m pytest tests/test_first_coaching_agent.py tests/test_coach_turn_agent.py -q
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/application/coach \
  java-core/src/main/java/com/wuxl/englishcoach/api/coach \
  java-core/src/test/java/com/wuxl/englishcoach/api/coach/FirstCoachingSessionControllerTest.java \
  python-agent/app python-agent/tests/test_first_coaching_agent.py python-agent/app/static/index.html
git commit -m "feat: add first coaching session"
```

---

### Task 9: Coach Review and Progress Repositioning

**Files:**
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachReviewService.java`
- Create DTOs under `java-core/src/main/java/com/wuxl/englishcoach/api/coach/dto/`
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/coach/CoachController.java`
- Modify: `python-agent/app/static/index.html`
- Test: `java-core/src/test/java/com/wuxl/englishcoach/api/coach/CoachReviewControllerTest.java`

- [ ] **Step 1: Write failing review API test**

Expected endpoint:

```text
GET /api/coach/review?userId={id}&startDate=2026-05-01&endDate=2026-05-07
```

Expected response fields:

```text
conversationTurns
newMemoryCount
topRepeatedProblems
improvedExpressions
nextWeekPlan
```

- [ ] **Step 2: Run test to verify failure**

```bash
cd java-core
mvn test -Dtest=CoachReviewControllerTest
```

Expected: FAIL because endpoint does not exist.

- [ ] **Step 3: Implement CoachReviewService**

Aggregate from:

- `coach_turn` count
- `error_pattern` and `expression_gap` created/updated in date range
- highest `seen_count` active memory items

First version may return deterministic text without LLM.

- [ ] **Step 4: Update Progress UI**

In `index.html`, make `Progress` show coach review first and old dashboard stats second.

- [ ] **Step 5: Run tests and manual UI check**

```bash
cd java-core && mvn test -Dtest=CoachReviewControllerTest,CoachControllerTest,MemoryControllerTest
```

Manual check: Progress page shows coach-review fields after at least one coach turn.

- [ ] **Step 6: Commit**

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/application/coach \
  java-core/src/main/java/com/wuxl/englishcoach/api/coach \
  java-core/src/test/java/com/wuxl/englishcoach/api/coach/CoachReviewControllerTest.java \
  python-agent/app/static/index.html
git commit -m "feat: add coach review progress view"
```

---

### Task 10: Full Regression and Cleanup

**Files:**
- Modify as needed based on failures.
- Optional docs update: `README.md` only if run commands or product entry changed.

- [ ] **Step 1: Run Python tests**

```bash
cd python-agent
python -m pytest -q
```

Expected: all Python tests pass.

- [ ] **Step 2: Run Java focused tests**

```bash
cd java-core
mvn test -Dtest=SchemaMigrationTest,MemoryControllerTest,CoachControllerTest,FirstCoachingSessionControllerTest,CoachReviewControllerTest
```

Expected: all focused Java tests pass.

- [ ] **Step 3: Run full Java tests if time permits**

```bash
cd java-core
mvn test
```

Expected: full Java suite passes. If not, document failing tests and whether failures pre-existed.

- [ ] **Step 4: Manual smoke test**

Start services:

```bash
cd java-core && mvn spring-boot:run
cd python-agent && uvicorn app.main:app --reload --port 8000
```

Smoke path:

```text
Open http://localhost:8000
Create/load user
Complete First Coaching Session
Start Today Coach
Submit: I need prepare the demo.
Confirm Saved note renders
Submit another missing-to sentence
Confirm 1-minute drill suggestion renders
Complete mini drill
Confirm Priority Memory updates
Open Progress and confirm Coach Review summary
```

- [ ] **Step 5: Update README if needed**

Only update if the app entry point or run instructions changed materially.

- [ ] **Step 6: Final commit**

```bash
git add README.md python-agent/app/static/index.html java-core/src/main java-core/src/test python-agent/app python-agent/tests
git commit -m "chore: finalize coach cockpit overhaul"
```

Skip this commit if there are no cleanup/doc changes after previous task commits.

## Final Verification Checklist

- [ ] Python tests pass: `python -m pytest -q`
- [ ] Java focused tests pass for new coach/memory behavior.
- [ ] Full Java suite has been run or any skipped reason is documented.
- [ ] Manual smoke test completes the First Coaching -> Today Coach -> Saved note -> Drill -> Memory -> Progress loop.
- [ ] Old demo pages are still reachable as secondary views during migration.
- [ ] No unrelated vocabulary migration or generated word-list files were committed as part of this overhaul.
