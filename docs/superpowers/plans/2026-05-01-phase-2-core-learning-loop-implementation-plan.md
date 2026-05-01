# Phase 2: Core Learning Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Implement the complete learning loop — Placement → Daily Plan → Study Session → Mastery Update → Weekly Review — in java-core, with seed learning content and all supporting domain rules.

**Architecture:** Follow the existing layered structure (api → application → domain → infrastructure). Domain rules live in dedicated policy/scorer/state-machine classes, not inside services. Each task delivers a testable vertical slice.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, PostgreSQL, Flyway, JUnit 5, H2 (test).

**Design references:**
- DB Schema: `docs/superpowers/specs/2026-03-25-java-core-db-schema.md`
- API Contracts: `docs/superpowers/specs/2026-03-25-java-core-api-contracts.md`
- Domain Rules: `docs/superpowers/specs/2026-03-25-java-core-domain-rules.md`
- Task Breakdown: `docs/superpowers/specs/2026-03-26-java-core-task-breakdown.md`

---

## 0. File Structure Map

### New migrations
- Create: `java-core/src/main/resources/db/migration/V2__create_learning_and_session_tables.sql`
- Create: `java-core/src/main/resources/db/migration/V3__create_plan_and_review_tables.sql`
- Create: `java-core/src/main/resources/db/migration/V4__seed_learning_items.sql`

### Content module
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/content/ContentController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/content/dto/LearningItemSummaryResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/content/dto/LearningItemDetailResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/content/ContentService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/content/LearningItem.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/content/LearningItemDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/content/LearningItemMapper.java`

### Placement module
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/placement/PlacementController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/placement/dto/PlacementAssessmentRequest.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/placement/dto/PlacementAssessmentResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/placement/PlacementService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/placement/PlacementScorer.java`

### Daily Plan module
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/DailyPlanController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/dto/GenerateDailyPlanRequest.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/plan/dto/DailyPlanResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/plan/DailyPlanService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/plan/DailyLoadPolicy.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/plan/ReviewPriorityCalculator.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/plan/NewItemSelectionPolicy.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/plan/DailyPlanSnapshotDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/plan/DailyPlanItemDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/plan/DailyPlanSnapshotMapper.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/plan/DailyPlanItemMapper.java`

### Study Session + Mastery module
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/StudySessionController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/dto/StartStudySessionRequest.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/dto/SubmitAttemptsRequest.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/dto/CompleteStudySessionRequest.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/session/dto/StudySessionDetailResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/mastery/MasteryController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/mastery/dto/MasteryStateResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/mastery/dto/DueReviewItemResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/session/StudySessionService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/mastery/MasteryStateService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/mastery/MasteryStateMachine.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/mastery/ScoreUpdatePolicy.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/mastery/NextReviewPolicy.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/session/StudySessionDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/session/AttemptLogDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/mastery/MasteryStateDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/session/StudySessionMapper.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/session/AttemptLogMapper.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/mastery/MasteryStateMapper.java`

### Weekly Review module
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/review/WeeklyReviewController.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/review/dto/GenerateWeeklyReviewRequest.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/api/review/dto/WeeklyReviewResponse.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/application/review/WeeklyReviewService.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/domain/review/WeeklyReviewAggregator.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/review/WeeklyReviewSnapshotDO.java`
- Create: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/review/WeeklyReviewSnapshotMapper.java`

### Modify existing files
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/common/enums/ErrorCodeEnum.java` — add new error codes
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/user/dto/CreateUserProfileRequest.java` — add missing fields (subGoals, preferredModes, etc.)
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/api/user/dto/UserProfileResponse.java` — add level fields
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/application/user/UserProfileService.java` — handle new fields
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/domain/user/UserProfile.java` — add level fields
- Modify: `java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user/UserProfileDO.java` — add level/preference columns
- Modify: `java-core/src/main/resources/db/migration/V1__create_user_profile.sql` — expand user_profile columns

### Test files
- Create: `java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence/SchemaMigrationTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/api/content/ContentControllerTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/domain/placement/PlacementScorerTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/api/placement/PlacementControllerTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/domain/plan/DailyLoadPolicyTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/domain/plan/ReviewPriorityCalculatorTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/api/plan/DailyPlanControllerTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/domain/mastery/MasteryStateMachineTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/domain/mastery/ScoreUpdatePolicyTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/api/session/StudySessionControllerTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/api/mastery/MasteryControllerTest.java`
- Create: `java-core/src/test/java/com/wuxl/englishcoach/api/review/WeeklyReviewControllerTest.java`

---

## Task 1: Expand DB schema and seed data

**Depends on:** Phase 1 complete
**Delivers:** All 10 tables migrated, seed learning items loaded

### Step 1: Expand user_profile table

Modify `V1__create_user_profile.sql` to add columns per the DB schema spec:

```sql
-- add to existing user_profile table:
-- sub_goals jsonb not null default '[]'::jsonb
-- overall_level varchar(16)
-- vocab_level varchar(16)
-- grammar_level varchar(16)
-- reading_level varchar(16)
-- output_level varchar(16)
-- preferred_modes jsonb not null default '[]'::jsonb
-- motivation_style varchar(32)
-- fatigue_tolerance varchar(32)
```

Update `UserProfileDO`, `UserProfile`, `UserProfileResponse`, `CreateUserProfileRequest` to match.

### Step 2: Create V2 migration — learning_item, mastery_state, study_session, attempt_log

Tables per DB schema spec section 4.2–4.5. Include indexes and constraints.

### Step 3: Create V3 migration — daily_plan_snapshot, daily_plan_item, weekly_review_snapshot

Tables per DB schema spec section 4.6, 4.9–4.10. Include indexes and constraints.

### Step 4: Create V4 migration — seed learning items

Insert ~20 seed items covering:
- Types: WORD, PHRASE, SENTENCE, GRAMMAR_POINT
- Themes: daily_life, workplace, travel, time_management
- Difficulty range: 1–7
- Each with meaning_zh, tags, examples (JSONB)

### Step 5: Write schema migration test

Verify all 10 tables exist, key constraints work, seed data is queryable.

### Step 6: Commit

```bash
git add java-core/src/main/resources/db/migration java-core/src/main/java/com/wuxl/englishcoach/domain/user java-core/src/main/java/com/wuxl/englishcoach/api/user java-core/src/main/java/com/wuxl/englishcoach/application/user java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/user java-core/src/test/java/com/wuxl/englishcoach/infrastructure/persistence
git commit -m "feat: expand db schema for core learning loop"
```

---

## Task 2: Content API — learning item queries

**Depends on:** Task 1 (learning_item table + seed data)
**Delivers:** GET /api/content/items, GET /api/content/items/{itemId}

### Step 1: Create LearningItemDO + LearningItemMapper

Fields match DB schema. Mapper extends BaseMapper, add custom select with type/theme/difficulty filters.

### Step 2: Create LearningItem domain record + ContentService

Service handles list query (with pagination + filters) and detail query.

### Step 3: Create DTOs + ContentController

- `LearningItemSummaryResponse` (id, itemCode, type, content, meaningZh, difficulty, theme, status)
- `LearningItemDetailResponse` (adds tags, examples, relatedItemCodes, createdAt, updatedAt)
- Controller: GET /api/content/items (paginated), GET /api/content/items/{itemId}

### Step 4: Write ContentControllerTest

Tests: list returns paginated results, filter by type works, detail returns full fields, 404 for missing item.

### Step 5: Commit

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/content java-core/src/main/java/com/wuxl/englishcoach/application/content java-core/src/main/java/com/wuxl/englishcoach/domain/content java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/content java-core/src/test/java/com/wuxl/englishcoach/api/content
git commit -m "feat: add content api for learning item queries"
```

---

## Task 3: Placement assessment

**Depends on:** Task 1 (study_session table), Task 1 (expanded user_profile)
**Delivers:** POST /api/placement/assess

### Step 1: Implement PlacementScorer domain class

Pure logic, no framework dependencies. Implements:
- Section score calculation (accuracy - hint_penalty - slow_penalty)
- Level mapping (score → A1-/A1/A1+/A2/A2+/B1/B1+/B2)
- Overall score (weighted: vocab 0.30, grammar 0.25, reading 0.20, output 0.25)
- Weaknesses extraction (score < 0.50 checks, KNOW_BUT_CANNOT_USE)
- Suggested daily rhythm (based on overallLevel + outputLevel)

### Step 2: Write PlacementScorerTest

Test cases: high accuracy → high level, low output → OUTPUT_WEAKNESS, hints → slight penalty.

### Step 3: Create DTOs

- `PlacementAssessmentRequest` (userId, answers: List<PlacementAnswerRequest>)
- `PlacementAnswerRequest` (section, questionId, result, responseText, responseTimeMs, hintUsed)
- `PlacementAssessmentResponse` (overallLevel, vocabLevel, grammarLevel, readingLevel, outputLevel, weaknesses, suggestedDailyRhythm)

### Step 4: Implement PlacementService

- Validate user exists
- Validate answers not empty
- Call PlacementScorer
- Update user_profile level fields
- Create PLACEMENT type study_session record
- Return assessment response

### Step 5: Implement PlacementController

POST /api/placement/assess → PlacementService

### Step 6: Write PlacementControllerTest

Tests: successful assessment updates user levels, empty answers → 4094, invalid userId → 4041.

### Step 7: Commit

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/placement java-core/src/main/java/com/wuxl/englishcoach/application/placement java-core/src/main/java/com/wuxl/englishcoach/domain/placement java-core/src/test/java/com/wuxl/englishcoach/domain/placement java-core/src/test/java/com/wuxl/englishcoach/api/placement
git commit -m "feat: add placement assessment"
```

---

## Task 4: Daily plan generation

**Depends on:** Task 2 (content queries), Task 3 (placement → user levels)
**Delivers:** POST /api/plans/daily:generate, GET /api/plans/daily

### Step 1: Create DO + Mappers for daily_plan_snapshot and daily_plan_item

### Step 2: Implement DailyLoadPolicy domain class

Pure logic. Determines new/review/output counts based on:
- dailyMinutes → base tier
- Recent completion rate, accuracy, fatigue → adjustments
- Overdue review volume → adjustments
- Output: loadDecision (LIGHT/NORMAL/BOOST), counts

### Step 3: Implement ReviewPriorityCalculator domain class

Pure logic. Per-item priority:
```
priority = 0.30 * due_score + 0.25 * forget_risk + 0.20 * recent_error_score
         + 0.15 * output_gap_score + 0.10 * theme_weight
         - 0.10 * recent_exposure_penalty
```

### Step 4: Implement NewItemSelectionPolicy domain class

Select new items by: goal alignment, theme continuity, difficulty proximity, not already in mastery_state.

### Step 5: Implement DailyPlanService

- Load user profile
- Calculate daily load (DailyLoadPolicy)
- Select review items (ReviewPriorityCalculator, from mastery_state where due)
- Select new items (NewItemSelectionPolicy)
- Assemble plan with rationale
- Persist daily_plan_snapshot + daily_plan_items
- Prevent duplicate generation (userId + planDate + planType)

### Step 6: Create DTOs + DailyPlanController

- `GenerateDailyPlanRequest` (userId, planDate, planType)
- `DailyPlanResponse` (planCode, planDate, planType, status, newItems, reviewItems, outputTasks, rationale)
- Controller: POST /api/plans/daily:generate, GET /api/plans/daily

### Step 7: Write domain tests + controller test

- DailyLoadPolicyTest: high completion → boost, fatigue → light
- ReviewPriorityCalculatorTest: overdue + weak_output → highest priority
- DailyPlanControllerTest: generate + query flow, duplicate prevention

### Step 8: Commit

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/plan java-core/src/main/java/com/wuxl/englishcoach/application/plan java-core/src/main/java/com/wuxl/englishcoach/domain/plan java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/plan java-core/src/test/java/com/wuxl/englishcoach/domain/plan java-core/src/test/java/com/wuxl/englishcoach/api/plan
git commit -m "feat: add daily plan generation"
```

---

## Task 5: Study session + mastery update

**Depends on:** Task 4 (daily plan), Task 1 (mastery_state, attempt_log tables)
**Delivers:** Session start/attempts/complete/detail, mastery query/due-review

### Step 1: Create DOs + Mappers for study_session, attempt_log, mastery_state

### Step 2: Implement ScoreUpdatePolicy domain class

Per attempt, update scores:
- recognition_score: correct → +0.10, wrong → -0.08, hint correct → +0.04
- output_score: correct → +0.15, wrong → -0.12
- forget_risk: recent errors → +, streak correct → -, long gap → +
- correct_streak: correct → +1, wrong → reset to 0

### Step 3: Implement MasteryStateMachine domain class

State transitions:
- NEW → LEARNING (first attempt)
- LEARNING → REVIEWING (recognition >= 0.70, streak >= 2)
- LEARNING/REVIEWING → WEAK_OUTPUT (recognition >= 0.70 && output < 0.45)
- REVIEWING → MASTERED (recognition >= 0.85, output >= 0.70, streak >= 4)
- MASTERED → REVIEWING/WEAK_OUTPUT (consecutive errors)

### Step 4: Implement NextReviewPolicy domain class

Base intervals: 1d, 3d, 7d, 14d, 30d. Adjusted by performance.

### Step 5: Implement StudySessionService

- startSession: validate user, create study_session record
- submitAttempts: batch write attempt_log, update mastery_state per attempt (ScoreUpdatePolicy + MasteryStateMachine + NextReviewPolicy)
- completeSession: calculate accuracy, completionRate, update session record
- getSessionDetail: return session + attempts

### Step 6: Implement MasteryStateService

- query mastery_state with filters (userId, status, dueBefore, theme)
- due-review: find items where next_review_at <= now, order by priority

### Step 7: Create DTOs + Controllers

- StudySessionController: POST /api/sessions/start, POST /api/sessions/{id}/attempts, POST /api/sessions/{id}/complete, GET /api/sessions/{id}
- MasteryController: GET /api/mastery, GET /api/mastery/due-review

### Step 8: Write tests

- MasteryStateMachineTest: all transitions
- ScoreUpdatePolicyTest: score changes per attempt type
- StudySessionControllerTest: full session flow
- MasteryControllerTest: query + due-review

### Step 9: Commit

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/session java-core/src/main/java/com/wuxl/englishcoach/api/mastery java-core/src/main/java/com/wuxl/englishcoach/application/session java-core/src/main/java/com/wuxl/englishcoach/application/mastery java-core/src/main/java/com/wuxl/englishcoach/domain/mastery java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/session java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/mastery java-core/src/test/java/com/wuxl/englishcoach/domain/mastery java-core/src/test/java/com/wuxl/englishcoach/api/session java-core/src/test/java/com/wuxl/englishcoach/api/mastery
git commit -m "feat: add study session and mastery update"
```

---

## Task 6: Weekly review

**Depends on:** Task 5 (session + mastery data)
**Delivers:** POST /api/reviews/weekly:generate, GET /api/reviews/weekly

### Step 1: Create DO + Mapper for weekly_review_snapshot

### Step 2: Implement WeeklyReviewAggregator domain class

Pure logic. Aggregates one week of data:
- completion_rate: completed_sessions / generated_plans
- study_minutes: sum of session durations
- new_items_count, review_items_count from sessions
- high_frequency_error_types: top 3 from attempt_log
- strongest/weakest themes: theme_score = 0.5*accuracy + 0.3*output - 0.2*error_rate
- next_week_suggestion: load/outputRatio/reviewFocus based on completion + accuracy + fatigue

### Step 3: Implement WeeklyReviewService

- Validate user exists
- Aggregate week data
- Persist weekly_review_snapshot
- Prevent duplicate (userId + week range)

### Step 4: Create DTOs + WeeklyReviewController

- `GenerateWeeklyReviewRequest` (userId, weekStartDate, weekEndDate)
- `WeeklyReviewResponse` (all aggregated fields)
- Controller: POST /api/reviews/weekly:generate, GET /api/reviews/weekly

### Step 5: Write tests

- WeeklyReviewService aggregation test
- WeeklyReviewControllerTest: generate + query

### Step 6: Commit

```bash
git add java-core/src/main/java/com/wuxl/englishcoach/api/review java-core/src/main/java/com/wuxl/englishcoach/application/review java-core/src/main/java/com/wuxl/englishcoach/domain/review java-core/src/main/java/com/wuxl/englishcoach/infrastructure/persistence/review java-core/src/test/java/com/wuxl/englishcoach/api/review
git commit -m "feat: add weekly review"
```

---

## Task 7: Full test suite verification

**Depends on:** All previous tasks
**Delivers:** All tests green, end-to-end smoke test pass

### Step 1: Run full test suite

```bash
cd java-core && mvn test
```

### Step 2: Fix any integration issues

### Step 3: Update README with Phase 2 smoke test steps

### Step 4: Final commit

```bash
git add -A
git commit -m "chore: verify phase-2 full test suite"
```

---

## Recommended implementation order

1. Task 1: DB schema + seed data (foundation for everything)
2. Task 2: Content API (needed by daily plan)
3. Task 3: Placement (sets user levels, needed by daily plan)
4. Task 4: Daily plan (needs content + mastery_state)
5. Task 5: Study session + mastery (needs daily plan)
6. Task 6: Weekly review (needs session data)
7. Task 7: Verification

## Notes for execution

- Domain rules (scorer, policy, state machine) must have independent unit tests before service integration.
- JSONB columns use MyBatis-Plus type handlers or manual serialization — keep consistent with existing JacksonConfig.
- All `*_code` fields generated by application layer (UUID-based).
- `updated_at` maintained by application layer, not DB triggers.
- Do NOT implement memory writeback (Phase 8) in this phase.
- Do NOT implement python-agent integration in this phase.
