# Code Review Handoff - 2026-05-04

This document is a handoff for a fresh code review session of `english-coach-agent` after the AI-native coach cockpit overhaul and the follow-up debugging work.

## 1. Repository State Summary

Current `main` head at the time of this handoff:

```text
e7968c9 update
979a8bf 词库数据
6686eaf feat: add coach review progress view
c800a4d feat: add first coaching session
06a750c feat: introduce today coach cockpit UI
dcf0712 feat: add coach session orchestration
c220555 feat: expose priority learner memory
9e71633 feat: add typed coach analysis client
c7be368 feat: add structured coach turn analysis
a86912e feat: add memory priority and drill rules
4b92d36 feat: add coach memory persistence schema
```

The coach cockpit feature was merged into `main` and pushed earlier. After that, additional vocabulary-data commits appear to have been added (`979a8bf`, `e7968c9`). There are also uncommitted diagnostic changes from the HTTP debugging session.

Current uncommitted files observed before writing this handoff:

```text
 M java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java
 M java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/CoachTurnAnalysisRequest.java
 M java-core/src/test/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClientTest.java
 M python-agent/app/api/coach_routes.py
 M python-agent/app/main.py
?? tatoeba/
```

Treat the Java/Python changes above as **temporary diagnostic/debugging changes**, not necessarily final production fixes. Do not commit them without review.

## 2. Original Feature Scope That Needs Review

The merged feature implemented an AI-native coach cockpit across Java, Python, database, and static UI.

Main feature areas:

1. Coach/memory persistence schema
2. Memory priority and drill suggestion policy
3. Python structured coach turn analysis endpoint
4. Java typed Python-agent client
5. Java priority memory API
6. Java coach session orchestration API
7. Static Today Coach cockpit UI
8. First Coaching Session flow
9. Coach Review / Progress view
10. Full regression and cleanup

Important merged commits:

```text
4b92d36 feat: add coach memory persistence schema
a86912e feat: add memory priority and drill rules
c7be368 feat: add structured coach turn analysis
9e71633 feat: add typed coach analysis client
c220555 feat: expose priority learner memory
dcf0712 feat: add coach session orchestration
06a750c feat: introduce today coach cockpit UI
c800a4d feat: add first coaching session
6686eaf feat: add coach review progress view
```

The feature was initially developed in a worktree and then fast-forward merged into `main`; the worktree and feature branch were cleaned up.

## 3. Known High-Priority Issue: Java -> Python Coach Turn HTTP Body Is Empty

### Symptom

Calling Java endpoint:

```http
POST http://localhost:8080/api/coach/sessions/1/turns
Content-Type: application/json

{"mode":"CHAT","message":"I need prepare the demo."}
```

returns fallback:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "coachReply": "Tell me more about that.",
    "savedNotes": [],
    "priorityMemory": { "items": [] },
    "drillSuggestion": null
  }
}
```

### Java-side behavior

`CoachSessionService.submitTurn()` calls:

```java
CoachTurnAnalysisResponse analysis = pythonAgentClient.analyzeCoachTurn(new CoachTurnAnalysisRequest(
        request.mode(),
        request.message(),
        Collections.emptyList()
));
```

Then falls back when `analysis == null` or `analysis.coachReply()` is blank:

```java
String coachReply = analysis != null && analysis.coachReply() != null && !analysis.coachReply().isBlank()
        ? analysis.coachReply()
        : "Tell me more about that.";
```

The Java log after temporary diagnostics showed non-empty payload was generated:

```text
Calling python-agent turn analysis payload={"mode":"CHAT","message":"Tell me more about that.","recent_memory":[]}
```

But Python received a request with `Content-Length` set and an empty body.

### Python-side evidence

Temporary raw ASGI body logging in `python-agent/app/main.py` produced:

```text
Raw incoming body path=/api/coach/turn/analyze content_type=application/json content_length=49 body_len=0 body=
```

Earlier Python validation logging showed:

```text
Request validation failed path=/api/coach/turn/analyze method=POST content_type=application/json errors=[{'type': 'missing', 'loc': ('body',), 'msg': 'Field required', 'input': None}] body=
```

This proves FastAPI is seeing no body. Because `content_length` is nonzero while `body_len=0`, the issue is likely at the Java HTTP client/runtime transport layer, not Pydantic field naming.

### What was tried

1. Added `@JsonProperty("recent_memory")` to Java `CoachTurnAnalysisRequest.recentMemory`.
2. Changed Java client from `.body(request)` to explicit JSON string body.
3. Changed Java client from string body to explicit UTF-8 bytes with `.contentLength(payloadBytes.length).body(payloadBytes)`.
4. Added Python raw ASGI body logging before FastAPI validation.

Even after the byte-array body attempt, Python still logged nonzero content length but `body_len=0`.

### Temporary changed files from this debugging

These files have debugging changes and should be reviewed/reverted/refined:

```text
java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java
java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/CoachTurnAnalysisRequest.java
java-core/src/test/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClientTest.java
python-agent/app/api/coach_routes.py
python-agent/app/main.py
```

### Suggested next debugging steps

A fresh reviewer should not keep patching blindly. Suggested minimal isolation:

1. Write a standalone Java test or small main using `java.net.http.HttpClient` to call `http://localhost:8000/api/coach/turn/analyze` with a literal JSON string.
2. Compare with Spring `RestClient` in the same runtime.
3. Confirm whether the issue only happens when Java is launched from IntelliJ, since `jps -lvm` showed the Spring app running under IntelliJ with an IDEA javaagent.
4. Confirm no local HTTP proxy, filter, firewall, security agent, or IntelliJ HTTP instrumentation is interfering.
5. Add request-body assertions to an integration test that uses a real ASGI/uvicorn target if feasible, not just Java `HttpServer`.

Possible alternatives if Spring `RestClient` remains suspect:

- Use `RestTemplate` for this one client.
- Use Java 17 `HttpClient` directly for Python-agent calls.
- Use `WebClient` if reactive dependency is acceptable.

Do not keep a raw-body logging middleware in production as-is; it logs user text and can leak sensitive data.

## 4. Vocabulary Migration Issue

The user introduced or generated vocabulary seed migrations:

```text
V4__seed_vocabulary_part1.sql
V5__seed_vocabulary_part2.sql
...
V20__seed_vocabulary_part17.sql
```

Each file contains roughly 5000 rows and is around 700-790 KB.

### Flyway behavior

With current config:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

Flyway runs pending `V*.sql` migrations once per database and records them in `flyway_schema_history`; it does not rerun successful migrations on every startup.

### Versioning concern

The original project had:

```text
V4__seed_learning_items.sql
```

The user manually removed its `flyway_schema_history` row and reportedly cleared the old inserted data. The working tree then had `V4__seed_learning_items.sql` removed and `V4__seed_vocabulary_part1.sql` introduced.

This is risky for shared environments because version 4 semantics changed. Safer long-term options:

1. For an already-initialized DB: add new versions after the current latest version, e.g. `V22__seed_vocabulary_part1.sql` onward.
2. For a fresh DB only: rebuilding the migration line may be acceptable, but every environment must agree.

### Data-length failure

Startup reached V7 and failed:

```text
SQL State  : 22001
Message    : ERROR: value too long for type character varying(255)
Location   : db/migration/V7__seed_vocabulary_part4.sql
```

Root cause: `learning_item.meaning_zh varchar(255)` is too small for vocabulary definitions.

Scan found 10 oversized `meaning_zh` values across V4-V20:

```text
V7__seed_vocabulary_part4.sql   line 3096  w_018092  cross-presentation       meaning_zh=258
V9__seed_vocabulary_part6.sql   line 3767  w_028763  food containers          meaning_zh=259
V9__seed_vocabulary_part6.sql   line 3772  w_028768  food labeling            meaning_zh=266
V10__seed_vocabulary_part7.sql  line 3275  w_033271  hand sb. sth.            meaning_zh=280
V11__seed_vocabulary_part8.sql  line 132   w_035128  historical biography     meaning_zh=258
V11__seed_vocabulary_part8.sql  line 918   w_035914  How?                     meaning_zh=278
V11__seed_vocabulary_part8.sql  line 1816  w_036812  illustrated book         meaning_zh=258
V18__seed_vocabulary_part15.sql line 3060  w_073056  telephone hotline        meaning_zh=258
V19__seed_vocabulary_part16.sql line 3746  w_078742  valproate-sodium         meaning_zh=259
V20__seed_vocabulary_part17.sql line 620   w_080616  West Indian manatee      meaning_zh=259
```

Recommended fix for already-run databases: add migration between V6 and V7, for example:

```text
V6.1__widen_learning_item_meaning_zh.sql
```

with:

```sql
alter table learning_item
    alter column meaning_zh type text;
```

For brand-new databases, also consider updating `V2__expand_user_profile_and_create_core_tables.sql` to define `meaning_zh text not null`, but do not modify V2 in environments where it has already been applied unless the DB is recreated or Flyway history is handled deliberately.

## 5. Areas That Need Code Review

### 5.1 Java API validation and fallback behavior

Files:

```text
java-core/src/main/java/com/wuxl/englishcoach/api/coach/CoachController.java
java-core/src/main/java/com/wuxl/englishcoach/api/coach/dto/SubmitCoachTurnRequest.java
java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java
java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/PythonAgentClient.java
```

Issues to review:

- `SubmitCoachTurnRequest` has no bean validation. Invalid external requests can propagate downstream and become confusing fallback behavior.
- `CoachSessionService` silently converts Python-agent failure to `Tell me more about that.`. This hides operational failures from users and makes debugging difficult.
- `PythonAgentClient` catches all exceptions and returns null. This is useful for resilience but poor for observability. Consider richer error reporting or response metadata.
- The fallback phrase is identical to Python fallback, which makes it hard to know where fallback occurred.

Recommended direction:

- Add `@NotBlank` to mode/message.
- Add `@Valid` in controller.
- Return a response flag such as `analysisSource` or `fallbackReason` during development, or at least log with request/session identifiers.
- Distinguish Java fallback from Python fallback text.

### 5.2 Java/Python contract naming

Files:

```text
java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/CoachTurnAnalysisRequest.java
java-core/src/main/java/com/wuxl/englishcoach/infrastructure/llm/dto/CoachTurnAnalysisResponse.java
python-agent/app/api/dto.py
```

Review points:

- Java request originally used `recentMemory`; Python expects `recent_memory`.
- Java response uses `@JsonProperty` for snake_case response fields; request should mirror this.
- Contract tests should assert exact JSON field names, not just that body contains the message.

### 5.3 Python structured output robustness

Files:

```text
python-agent/app/agents/coach_agent.py
python-agent/app/services/llm_service.py
python-agent/app/api/dto.py
```

Risks:

- `llm_service.structured()` assumes `response.choices[0].message.content` is JSON string parseable by `json.loads`.
- If LiteLLM returns provider-specific structured object, empty content, invalid JSON, or tool-call style data, this returns `None` and falls back.
- Python route can return 500 if constructing `CoachTurnAnalyzeResponse(**result)` fails after a malformed LLM response. Some errors are caught in `llm_service`, but review coverage is needed.

Review points:

- Add tests for malformed structured LLM responses.
- Keep fallback deterministic but useful.
- Avoid logging raw learner text in production unless trace mode explicitly allows it.

### 5.4 Memory persistence and drill suggestion

Files:

```text
java-core/src/main/java/com/wuxl/englishcoach/application/memory/MemoryService.java
java-core/src/main/java/com/wuxl/englishcoach/domain/memory/MemoryPriorityPolicy.java
java-core/src/main/java/com/wuxl/englishcoach/domain/memory/DrillSuggestionPolicy.java
java-core/src/main/resources/db/migration/V21__create_coach_memory_tables.sql
```

Review points:

- `MemoryService.mergeSavedErrorPattern()` only handles error patterns. Expression gaps from Python response are currently ignored by Java persistence.
- JSON list fields are manually appended. Check duplicate handling, max size, malformed JSON behavior, and data growth.
- `next_drill_at` scheduling appears basic or absent. Drill suggestion policy checks status/count but does not appear to update scheduling.
- Review whether `pattern_key` uniqueness by `(user_id, pattern_key)` is enough for model-generated keys.

### 5.5 First Coaching Session integration

Files:

```text
java-core/src/main/java/com/wuxl/englishcoach/application/coach/CoachSessionService.java
python-agent/app/agents/coach_agent.py
python-agent/app/api/coach_routes.py
```

Known limitation:

- Java `completeFirstSession()` currently uses deterministic fallback `A2-B1`; it does not appear to call Python `/api/coach/first/analyze`.

Review question:

- Is first-session analysis intended to be AI-driven now, or is Java deterministic fallback acceptable for this iteration?

### 5.6 UI quality and workflow

File:

```text
python-agent/app/static/index.html
```

Review points:

- UI is a single static HTML file with growing JavaScript. Review maintainability and state handling.
- Check that Today Coach default view, First Coaching Session panel, Priority Memory, Progress, and Legacy navigation all work after real services are running.
- Verify UI does not submit prior coach reply as next user message. During debugging, Java log showed payload message `Tell me more about that.` instead of original `I need prepare the demo.` This may be a user/test artifact or a UI state bug.

### 5.7 Database migration strategy

Files:

```text
java-core/src/main/resources/db/migration/*.sql
```

Review points:

- There is migration version churn around V4-V20 vocabulary seed data.
- The migration directory contains both schema and large seed data. Consider whether vocabulary import should be a separate idempotent import job/tool rather than Flyway versioned migrations.
- If large vocabulary seed remains in Flyway, ensure version ordering does not conflict with existing deployed DBs.
- Add preflight validation for generated SQL against schema limits before migration files are produced.

## 6. Tests and Commands Recently Run

Recent successful tests before later debugging changes:

```powershell
cd python-agent
python -m pytest -p no:cacheprovider -q
# 25 passed

cd java-core
mvn test
# 88 tests, 0 failures on clean worktree before vocabulary migration churn
```

After diagnostic Java client changes:

```powershell
cd java-core
mvn test -Dtest=PythonAgentClientTest
# Tests run: 1, Failures: 0, Errors: 0
```

After Python logging changes:

```powershell
cd python-agent
python -m pytest -p no:cacheprovider tests/test_coach_turn_routes.py -q
# 1 passed
```

Caution: Full Java test in the main workspace may be affected by untracked vocabulary migration files and current database migration changes. A clean worktree or controlled migration directory is recommended for reliable review.

## 7. Suggested Prompt for New Code Review Session

Use something like this in a fresh session:

```text
Please perform a rigorous code review of the English Coach Agent project at C:\workspace\english-coach-agent.

Focus especially on:
1. Java -> Python agent HTTP integration for /api/coach/turn/analyze. Current symptom: Java logs a non-empty payload, but Python raw ASGI logging reports content_length > 0 and body_len=0, causing FastAPI 422 and Java fallback "Tell me more about that."
2. The temporary diagnostic changes in PythonAgentClient.java, CoachTurnAnalysisRequest.java, PythonAgentClientTest.java, python-agent/app/main.py, and python-agent/app/api/coach_routes.py. Decide what should become permanent, what should be reverted, and what deeper fix is required.
3. Flyway migration strategy after vocabulary seed changes V4-V20, especially meaning_zh varchar(255) overflow and version reuse of V4.
4. End-to-end correctness of Today Coach, First Coaching Session, Priority Memory, Drill Suggestion, and Progress/Coach Review.
5. Missing validations, hidden fallbacks, and observability gaps.

Do not modify unrelated untracked files under tatoeba/, tools/, Chinese vocabulary directories, or generated vocabulary migrations unless explicitly needed for the review.

Start with findings ordered by severity, with file/line references and concrete reproduction evidence. Then propose a minimal remediation plan.
```

## 8. Do-Not-Touch / Caution List

The user previously emphasized not to accidentally include unrelated files. Be careful with:

```text
tatoeba/
tools/
Chinese vocabulary directory
large generated vocabulary split migrations unless explicitly reviewing migration strategy
```

Before committing anything, inspect:

```powershell
git status --short
git diff --name-only
git diff --stat
```

## 9. Immediate Recommendation

Do not push the current diagnostic changes as-is.

Recommended immediate sequence for the next session:

1. Preserve this handoff.
2. Run a minimal standalone Java `HttpClient` call to Python to see if uvicorn receives body bytes.
3. If standalone Java works, isolate Spring `RestClient` behavior or IntelliJ runtime interaction.
4. If standalone Java also fails, inspect local proxy/security/HTTP stack/environment.
5. Revert or guard raw body logging before production commit.
6. Add permanent contract/integration tests once root cause is known.
