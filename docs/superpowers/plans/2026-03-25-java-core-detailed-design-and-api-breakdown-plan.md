# Java Core 详细设计与接口拆解计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不写代码的前提下，把 `java-core` 的职责范围完整拆解为可执行设计文档，包括数据库表设计、模块边界、API 契约、DTO 范围、Mapper 职责、校验规则和实施任务切片。

**Architecture:** `java-core` 作为模块化单体，负责所有确定性、可解释、可持久化的学习核心真相，包括用户画像、定级评分、日计划、复习调度、掌握状态、学习会话和周复盘。技术路线采用 Spring Boot + MyBatis-Plus + Flyway，通过清晰的 Controller → Application Service → Domain Service → Mapper 分层维持边界。

**Tech Stack:** Java 17/21、Spring Boot 3、Maven、MyBatis-Plus、PostgreSQL、Flyway、Bean Validation、JUnit 5、Testcontainers。

---

## 0. 文档目标

这份文档**不是代码实现文档**。

它是 `java-core` 的编码前拆解文档，目的是把下面这些事情先定义清楚：
- 需要哪些表
- 每张表存什么
- 每个接口负责什么
- 每个接口的请求和响应字段
- 参数校验规则
- 模块边界
- Mapper 的职责
- Service 的职责
- 后续实现任务怎么拆

目标很简单：**在写 Java 代码前，尽量把歧义消灭掉。**

---

## 1. Java Core 的职责边界

`java-core` 负责系统中所有**确定性、可解释、可追踪、可持久化**的业务真相。

### 属于 `java-core` 的内容
- 用户画像的存储与更新
- 定级评分
- 学习项存储
- 日计划生成
- 复习调度
- 掌握状态迁移
- 学习会话持久化
- 单题尝试记录持久化
- 周复盘聚合
- Python 侧生成的结构化反思结果回写存储

### 不属于 `java-core` 的内容
- 教练自然语言文案生成
- 自由文本解释生成
- 创造性练习生成
- 多模型路由
- 反思推理本身
- correction loop 的决策过程本身

这些属于 `python-agent`。

---

## 2. java-core 内部模块拆分

推荐包结构：

```text
com.wuxl.englishcoach
├─ api
│  ├─ user
│  ├─ placement
│  ├─ content
│  ├─ plan
│  ├─ session
│  ├─ review
│  └─ memory
├─ application
│  ├─ user
│  ├─ placement
│  ├─ content
│  ├─ plan
│  ├─ scheduler
│  ├─ mastery
│  ├─ session
│  ├─ feedback
│  ├─ review
│  └─ memory
├─ domain
│  ├─ user
│  ├─ placement
│  ├─ content
│  ├─ plan
│  ├─ scheduler
│  ├─ mastery
│  ├─ session
│  ├─ feedback
│  └─ review
├─ infrastructure
│  ├─ persistence
│  ├─ config
│  └─ support
└─ common
   ├─ dto
   ├─ enums
   ├─ exception
   └─ response
```

### 分层原则
- `api`：只做请求接收、参数校验触发、响应返回
- `application`：负责编排用例、事务边界和跨模块协作
- `domain`：只放纯业务规则、公式、状态机
- `infrastructure/persistence`：放 MyBatis-Plus DO、Mapper、自定义 SQL
- `common`：放枚举、异常、通用响应结构等

---

## 3. 数据库整体设计概览

第一版 MVP 建议包含 **8 张主表 + 2 张辅助表**。

### 主表
1. `user_profile`
2. `learning_item`
3. `mastery_state`
4. `study_session`
5. `attempt_log`
6. `weekly_review_snapshot`
7. `session_reflection`
8. `behavior_pattern_summary`

### 辅助表
9. `daily_plan_snapshot`
10. `daily_plan_item`

这里我建议 **辅助表也进入 MVP**。
原因很简单：
**日计划本身就是一个业务对象，不是临时算完就丢的中间结果。**
它需要可追踪、可解释、可复盘。

---

## 4. 按表拆解数据库设计

### 4.1 `user_profile`
用途：存用户身份、学习目标、学习节奏偏好和当前能力画像。

建议字段：
- `id` bigint / uuid，主键
- `user_code` varchar，外部用户标识，唯一
- `goal` varchar(32)
- `sub_goals` jsonb
- `daily_minutes` int
- `study_start_time` varchar(8)
- `review_time` varchar(8)
- `overall_level` varchar(16)
- `vocab_level` varchar(16)
- `grammar_level` varchar(16)
- `reading_level` varchar(16)
- `output_level` varchar(16)
- `preferred_modes` jsonb
- `motivation_style` varchar(32)
- `fatigue_tolerance` varchar(32)
- `status` varchar(16)
- `created_at` timestamp
- `updated_at` timestamp

索引建议：
- `user_code` 唯一索引

校验建议：
- `daily_minutes` 取值 5~180
- 时间字段统一 `HH:mm`

---

### 4.2 `learning_item`
用途：存受控的种子学习内容。

建议字段：
- `id`
- `item_code` varchar 唯一
- `type` varchar(32)
- `content` varchar(255)
- `meaning_zh` varchar(255)
- `difficulty` int
- `theme` varchar(64)
- `tags` jsonb
- `examples` jsonb
- `related_item_codes` jsonb
- `status` varchar(16)
- `created_at`
- `updated_at`

索引建议：
- `item_code` 唯一索引
- `type` 普通索引
- `theme` 普通索引
- `difficulty` 普通索引

---

### 4.3 `mastery_state`
用途：存某个用户对某个学习项的长期掌握真相。

建议字段：
- `id`
- `user_id`
- `learning_item_id`
- `seen_count`
- `correct_count`
- `wrong_count`
- `correct_streak`
- `recognition_score` numeric(5,2)
- `recall_score` numeric(5,2)
- `output_score` numeric(5,2)
- `memory_strength` numeric(5,2)
- `forget_risk` numeric(5,2)
- `last_seen_at`
- `next_review_at`
- `last_mode` varchar(32)
- `recommended_mode` varchar(32)
- `status` varchar(32)
- `created_at`
- `updated_at`

索引建议：
- (`user_id`, `learning_item_id`) 唯一索引
- (`user_id`, `next_review_at`) 复合索引
- (`user_id`, `status`) 复合索引

说明：
- 这是系统里最核心的长期状态表
- Python Agent 不应该直接改写它的业务规则

---

### 4.4 `study_session`
用途：记录一整次学习会话。

建议字段：
- `id`
- `session_code` varchar 唯一
- `user_id`
- `session_date` date
- `session_type` varchar(32)
- `status` varchar(16)
- `duration_min` int
- `new_items_count` int
- `review_items_count` int
- `accuracy` numeric(5,2)
- `completion_rate` numeric(5,2)
- `fatigue_feedback` varchar(32)
- `mood_feedback` varchar(32)
- `focus_theme` varchar(64)
- `started_at`
- `completed_at`
- `created_at`
- `updated_at`

索引建议：
- `session_code` 唯一索引
- (`user_id`, `session_date`) 索引
- (`user_id`, `session_type`) 索引

---

### 4.5 `attempt_log`
用途：记录单题级别尝试。

建议字段：
- `id`
- `attempt_code` varchar 唯一
- `user_id`
- `learning_item_id`
- `study_session_id`
- `mode` varchar(32)
- `result` varchar(16)
- `response_text` text
- `response_time_ms` int
- `hint_used` boolean
- `error_type` varchar(32)
- `error_details` jsonb
- `timestamp`
- `llm_explanation_snapshot` text
- `created_at`

索引建议：
- `attempt_code` 唯一索引
- (`user_id`, `learning_item_id`) 索引
- (`study_session_id`) 索引
- (`error_type`) 索引

---

### 4.6 `weekly_review_snapshot`
用途：存每周复盘结果，方便查询和审计。

建议字段：
- `id`
- `user_id`
- `week_start_date`
- `week_end_date`
- `completion_rate` numeric(5,2)
- `study_minutes` int
- `new_items_count` int
- `review_items_count` int
- `high_frequency_error_types` jsonb
- `strongest_themes` jsonb
- `weakest_themes` jsonb
- `next_week_suggestion` jsonb
- `created_at`

索引建议：
- (`user_id`, `week_start_date`, `week_end_date`) 唯一索引

---

### 4.7 `session_reflection`
用途：存 Python 生成、但由 Java 负责持久化的结构化会话反思。

建议字段：
- `id`
- `study_session_id`
- `user_id`
- `detected_patterns` jsonb
- `fatigue_signal` varchar(32)
- `recommended_adjustment` varchar(64)
- `coach_notes` text
- `confidence` numeric(5,2)
- `source` varchar(16)
- `created_at`

索引建议：
- (`user_id`, `study_session_id`) 索引

---

### 4.8 `behavior_pattern_summary`
用途：存多次会话汇总后形成的行为模式摘要。

建议字段：
- `id`
- `user_id`
- `summary_type` varchar(32)
- `patterns` jsonb
- `recommended_strategy` jsonb
- `confidence` numeric(5,2)
- `source` varchar(16)
- `created_at`

索引建议：
- (`user_id`, `summary_type`) 索引

---

### 4.9 `daily_plan_snapshot`
用途：持久化每日计划快照，保证可追踪和可复现。

建议字段：
- `id`
- `plan_code` varchar 唯一
- `user_id`
- `plan_date` date
- `plan_type` varchar(32)
- `status` varchar(16)
- `total_new_count` int
- `total_review_count` int
- `total_output_count` int
- `load_reason` jsonb
- `created_at`
- `updated_at`

索引建议：
- (`user_id`, `plan_date`, `plan_type`) 唯一索引

---

### 4.10 `daily_plan_item`
用途：存某个日计划中到底有哪些内容、按什么顺序、为什么被选中。

建议字段：
- `id`
- `daily_plan_snapshot_id`
- `learning_item_id`
- `item_role` varchar(32)
- `sequence_no` int
- `selection_reason` jsonb
- `priority_score` numeric(8,4)
- `recommended_mode` varchar(32)
- `created_at`

索引建议：
- (`daily_plan_snapshot_id`, `sequence_no`) 索引
- (`learning_item_id`) 索引

---

## 5. 建议尽早统一的枚举

建议先统一枚举，避免项目里到处飞字符串。

### 核心枚举
- `GoalType`：GENERAL、EXAM、SPEAKING、READING、WORKPLACE
- `LearningItemType`：WORD、PHRASE、SENTENCE、GRAMMAR
- `MasteryStatus`：NEW、LEARNING、REVIEWING、WEAK_OUTPUT、MASTERED、ARCHIVED
- `SessionType`：PLACEMENT、DAILY_LEARNING、QUICK_REVIEW、WEEKLY_REVIEW
- `SessionStatus`：STARTED、COMPLETED、ABORTED
- `AttemptResult`：CORRECT、WRONG、PARTIAL
- `ErrorType`：VOCAB_CONFUSION、COLLOCATION_ERROR、TENSE_ERROR、ARTICLE_ERROR、WORD_ORDER_ERROR、OUTPUT_WEAKNESS、UNKNOWN
- `PlanType`：NORMAL、LIGHT_REVIEW
- `PlanStatus`：GENERATED、STARTED、COMPLETED、SKIPPED
- `ItemRole`：WARMUP_REVIEW、REVIEW、NEW_LEARNING、OUTPUT_TASK、QUIZ

---

## 6. API 设计原则

### 通用原则
- 所有接口统一挂在 `/api` 下
- 响应格式统一
- Python 调用方和未来前端共享同一份契约
- 核心接口尽量可解释、可复现

### 建议的统一响应结构
```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

错误响应示例：
```json
{
  "code": 4001,
  "message": "invalid request",
  "data": null
}
```

---

## 7. 详细 API 拆解

## 7.1 用户画像接口

### 接口 1：创建用户画像
**Method：** `POST /api/users`

用途：
- 创建新的学习用户

请求体：
```json
{
  "userCode": "u_001",
  "goal": "GENERAL",
  "subGoals": ["daily_conversation"],
  "dailyMinutes": 20,
  "studyStartTime": "20:00",
  "reviewTime": "12:30",
  "preferredModes": ["review_first", "short_session"],
  "motivationStyle": "encouraging",
  "fatigueTolerance": "medium"
}
```

校验规则：
- `userCode` 必填，最长 64
- `goal` 必填
- `dailyMinutes` 必填，范围 5~180
- 时间字段必须符合 `HH:mm`

响应体：
```json
{
  "id": 1,
  "userCode": "u_001",
  "goal": "GENERAL",
  "dailyMinutes": 20,
  "studyStartTime": "20:00",
  "reviewTime": "12:30",
  "status": "ACTIVE"
}
```

---

### 接口 2：查询用户画像
**Method：** `GET /api/users/{userId}`

用途：
- 获取用户基础画像和当前等级摘要

响应字段建议包括：
- 用户基础信息
- 当前 level 字段
- 学习偏好字段
- 创建更新时间

---

### 接口 3：更新用户画像
**Method：** `PUT /api/users/{userId}`

用途：
- 更新用户学习偏好和时间设置

请求体：
- 与创建接口字段接近
- 只允许更新可变字段

---

## 7.2 定级接口

### 接口 4：提交定级评估
**Method：** `POST /api/placement/assess`

用途：
- 根据结构化答题结果计算初始能力画像

请求体：
```json
{
  "userId": 1,
  "answers": [
    {
      "section": "vocab",
      "questionId": "v_001",
      "result": "CORRECT",
      "responseTimeMs": 3200
    }
  ]
}
```

响应体：
```json
{
  "overallLevel": "A2+",
  "vocabLevel": "B1",
  "grammarLevel": "A2",
  "readingLevel": "A2+",
  "outputLevel": "A1+",
  "weaknesses": ["OUTPUT_WEAKNESS", "TENSE_ERROR"],
  "suggestedDailyRhythm": {
    "newItems": 6,
    "reviewItems": 12,
    "outputTasks": 2
  }
}
```

校验规则：
- `userId` 必填
- `answers` 不能为空

副作用：
- 更新 `user_profile` 中的等级字段
- 生成一条 `PLACEMENT` 类型的 `study_session`

---

## 7.3 内容接口

### 接口 5：学习项列表
**Method：** `GET /api/content/items`

Query 参数：
- `type`
- `theme`
- `difficultyMin`
- `difficultyMax`
- `page`
- `size`

用途：
- 查询种子学习项，供调试、内部检查和后续管理使用

---

### 接口 6：学习项详情
**Method：** `GET /api/content/items/{itemId}`

用途：
- 查询单个学习项详情、例句、标签和关联项

---

## 7.4 日计划接口

### 接口 7：生成日计划
**Method：** `POST /api/plans/daily:generate`

用途：
- 计算并持久化一份日计划

请求体：
```json
{
  "userId": 1,
  "planDate": "2026-03-25",
  "planType": "NORMAL"
}
```

响应体：
```json
{
  "planCode": "plan_20260325_001",
  "planDate": "2026-03-25",
  "planType": "NORMAL",
  "newItems": [
    {
      "itemId": 101,
      "content": "book a meeting",
      "itemRole": "NEW_LEARNING",
      "recommendedMode": "meaning_intro"
    }
  ],
  "reviewItems": [
    {
      "itemId": 12,
      "content": "take a break",
      "itemRole": "REVIEW",
      "priorityScore": 0.87,
      "selectionReason": {
        "dueScore": 0.9,
        "forgetRisk": 0.8,
        "outputGapScore": 0.6
      }
    }
  ],
  "outputTasks": [
    {
      "taskType": "sentence_building",
      "relatedItemIds": [12, 101]
    }
  ],
  "rationale": {
    "loadDecision": "normal",
    "whyReviewThese": ["overdue", "weak_output"],
    "whyNewCountIsThis": "daily_minutes_20_recent_accuracy_ok"
  }
}
```

副作用：
- 写入 `daily_plan_snapshot`
- 写入 `daily_plan_item`

---

### 接口 8：按日期查询日计划
**Method：** `GET /api/plans/daily`

Query 参数：
- `userId`
- `planDate`
- `planType`

用途：
- 获取已生成的日计划

---

## 7.5 学习会话接口

### 接口 9：开始学习会话
**Method：** `POST /api/sessions/start`

用途：
- 在开始做题前先创建一条 session 记录

请求体：
```json
{
  "userId": 1,
  "sessionType": "DAILY_LEARNING",
  "planCode": "plan_20260325_001",
  "focusTheme": "workplace"
}
```

响应体：
```json
{
  "sessionId": 1001,
  "sessionCode": "sess_20260325_001",
  "status": "STARTED",
  "startedAt": "2026-03-25T20:01:10"
}
```

---

### 接口 10：批量提交尝试记录
**Method：** `POST /api/sessions/{sessionId}/attempts`

用途：
- 写入结构化答题记录
- 增量更新掌握状态，或标记为待最终汇总

请求体：
```json
{
  "attempts": [
    {
      "learningItemId": 12,
      "mode": "cn_to_en",
      "result": "WRONG",
      "responseText": "take rest",
      "responseTimeMs": 4100,
      "hintUsed": false,
      "errorType": "COLLOCATION_ERROR",
      "errorDetails": {
        "expected": "take a break",
        "actual": "take rest"
      }
    }
  ]
}
```

校验规则：
- `attempts` 不能为空
- `learningItemId`、`mode`、`result` 必填

副作用：
- 写入 `attempt_log`
- 更新 `mastery_state`

---

### 接口 11：完成学习会话
**Method：** `POST /api/sessions/{sessionId}/complete`

用途：
- 结束会话并计算本次会话统计字段

请求体：
```json
{
  "durationMin": 18,
  "fatigueFeedback": "medium",
  "moodFeedback": "focused"
}
```

响应体：
```json
{
  "sessionId": 1001,
  "status": "COMPLETED",
  "accuracy": 0.78,
  "completionRate": 0.92,
  "newItemsCount": 6,
  "reviewItemsCount": 11
}
```

---

### 接口 12：查询会话详情
**Method：** `GET /api/sessions/{sessionId}`

用途：
- 获取会话摘要和尝试明细，用于调试、回放和 Agent 上下文拼装

---

## 7.6 掌握状态与记忆接口

### 接口 13：按用户查询掌握状态
**Method：** `GET /api/mastery`

Query 参数：
- `userId`
- `status` 可选
- `dueBefore` 可选
- `theme` 可选
- `page`
- `size`

用途：
- 支持内部调试和 Agent 查询用户掌握状态

---

### 接口 14：查询当前到期复习项
**Method：** `GET /api/mastery/due-review`

Query 参数：
- `userId`
- `at` 可选时间点
- `limit`

用途：
- 返回当前应复习的候选项，并按优先级排序

---

### 接口 15：保存会话反思
**Method：** `POST /api/memory/session-reflections`

用途：
- 接收 Python 侧生成的结构化反思并持久化

请求体：
```json
{
  "sessionId": 1001,
  "userId": 1,
  "detectedPatterns": ["output_weakness", "fatigue_rise_after_15_min"],
  "fatigueSignal": "medium",
  "recommendedAdjustment": "reduce_new_items",
  "coachNotes": "user slows down on open-ended output tasks",
  "confidence": 0.81,
  "source": "python-agent"
}
```

---

### 接口 16：保存行为模式摘要
**Method：** `POST /api/memory/behavior-patterns`

用途：
- 持久化 Python 生成的滚动行为摘要

---

## 7.7 周复盘接口

### 接口 17：生成周复盘
**Method：** `POST /api/reviews/weekly:generate`

用途：
- 聚合一周学习行为并持久化周报

请求体：
```json
{
  "userId": 1,
  "weekStartDate": "2026-03-23",
  "weekEndDate": "2026-03-29"
}
```

响应体：
```json
{
  "completionRate": 0.86,
  "studyMinutes": 134,
  "newItemsCount": 31,
  "reviewItemsCount": 66,
  "highFrequencyErrorTypes": ["COLLOCATION_ERROR", "OUTPUT_WEAKNESS"],
  "strongestThemes": ["daily_life"],
  "weakestThemes": ["workplace"],
  "nextWeekSuggestion": {
    "load": "keep",
    "outputRatio": "increase"
  }
}
```

副作用：
- 写入 `weekly_review_snapshot`

---

### 接口 18：查询周复盘快照
**Method：** `GET /api/reviews/weekly`

Query 参数：
- `userId`
- `weekStartDate`
- `weekEndDate`

用途：
- 查询已生成的周复盘快照

---

## 8. 参数校验和错误处理规则

### 参数校验
建议在 Request DTO 层统一使用 Bean Validation。

通用规则：
- 必填字段用 `@NotNull` / `@NotBlank`
- 枚举字段需要校验合法性
- List/Array 字段要限制长度
- 时间/日期格式必须明确

### 错误码分类建议
- `4001` 请求参数非法
- `4002` 枚举值非法
- `4003` 时间/日期格式错误
- `4041` 用户不存在
- `4042` 会话不存在
- `4043` 计划不存在
- `4044` 学习项不存在
- `4091` 用户编码重复
- `4092` 同日计划重复生成
- `4093` 会话已完成，不允许重复完成
- `5001` 内部领域逻辑错误

---

## 9. Mapper 设计拆解

基础 CRUD 用 MyBatis-Plus，复杂查询用自定义 SQL。

### 9.1 UserProfileMapper
职责：
- 插入用户画像
- 更新用户画像
- 按 id 查询
- 按 user_code 查询

### 9.2 LearningItemMapper
职责：
- 按 type/theme/difficulty 查询列表
- 按 id 查询详情
- 按 item_code 查询

### 9.3 MasteryStateMapper
职责：
- 按 user + item upsert
- 查询到期复习项
- 按 status 查询
- 查询单个 user + item 的掌握状态
- 自定义优先级候选查询

### 9.4 StudySessionMapper
职责：
- 创建 session
- 更新 session 完成信息
- 查询 session 详情
- 按用户和时间范围查询 session 列表

### 9.5 AttemptLogMapper
职责：
- 批量写入 attempt
- 按 session 查询 attempt
- 按用户/日期范围聚合 attempt

### 9.6 DailyPlanSnapshotMapper
职责：
- 写入日计划快照
- 按 user/date/type 查询
- 更新计划状态

### 9.7 DailyPlanItemMapper
职责：
- 批量写入计划项
- 按 plan id 顺序查询计划项

### 9.8 WeeklyReviewSnapshotMapper
职责：
- 写入周复盘快照
- 按 user/week 范围查询

### 9.9 SessionReflectionMapper
职责：
- 写入会话反思
- 查询用户最近反思

### 9.10 BehaviorPatternSummaryMapper
职责：
- 写入行为模式摘要
- 按类型查询最近摘要

---

## 10. Service 设计拆解

### 10.1 UserProfileService
职责：
- 创建用户画像
- 查询用户画像
- 更新用户画像

### 10.2 PlacementService
职责：
- 校验定级输入
- 调用定级评分器
- 更新用户等级字段
- 持久化 placement session

### 10.3 ContentService
职责：
- 查询学习项列表
- 查询学习项详情

### 10.4 DailyPlanService
职责：
- 计算日负载
- 拉取复习候选项
- 拉取新学候选项
- 组装日计划快照和计划项
- 返回可解释的计划原因

### 10.5 MemorySchedulerService
职责：
- 计算 due score
- 计算 forget risk
- 计算 review priority
- 推荐下次复习时间和模式

### 10.6 MasteryStateService
职责：
- 初始化某个学习项的掌握状态
- 根据 attempt 更新掌握状态
- 维护下次复习时间

### 10.7 FeedbackAnalysisService
职责：
- 规范化错误类型
- 计算近期错误统计
- 为周复盘聚合提供支撑

### 10.8 StudySessionService
职责：
- 开始 session
- 写入 attempts
- 完成 session
- 汇总 session 统计信息

### 10.9 WeeklyReviewService
职责：
- 聚合一周统计
- 找出强势主题和弱势主题
- 计算下周节奏建议
- 持久化周复盘快照

### 10.10 MemoryWritebackService
职责：
- 持久化 Python 传回的 session reflection
- 持久化 Python 传回的 behavior summary

---

## 11. 在编码前就应该存在的领域规则文档

建议在写代码前，把下面这些规则单独写成文档：

1. `placement-scoring-rules.md`
2. `daily-load-policy.md`
3. `review-priority-formula.md`
4. `mastery-state-transition-rules.md`
5. `weekly-review-aggregation-rules.md`
6. `error-type-normalization-rules.md`

每份规则文档至少要写清：
- 输入
- 输出
- 判定规则
- 示例
- 边界情况
- 对应测试用例

---

## 12. 下一步建议输出的 java-core 文档集

在写代码前，我建议补齐这 6 份文档：

1. `java-core-db-schema.md`
2. `java-core-api-contracts.md`
3. `java-core-domain-rules.md`
4. `java-core-package-design.md`
5. `java-core-task-breakdown.md`
6. `java-core-test-strategy.md`

---

## 13. java-core 进一步实施拆解

这一部分依然是编码前拆解，不是代码实现。

### Phase A：基础定义文档
- [ ] 确认表清单
- [ ] 确认枚举清单
- [ ] 确认 API 清单
- [ ] 确认错误码清单
- [ ] 确认包结构

### Phase B：数据库设计文档
- [ ] 为 10 张表写 DDL 草案
- [ ] 定义索引和唯一约束
- [ ] 定义 JSONB 字段结构
- [ ] 定义审计字段和命名规范
- [ ] 定义 Flyway migration 切分策略

### Phase C：API 契约文档
- [ ] 把 1~18 号接口写成正式请求/响应契约
- [ ] 定义分页契约
- [ ] 定义统一响应包装
- [ ] 定义统一错误响应结构
- [ ] 定义 id/code 命名规范

### Phase D：领域规则文档
- [ ] 写定级评分规则
- [ ] 写日负载计算规则
- [ ] 写复习优先级公式规则
- [ ] 写掌握状态迁移规则
- [ ] 写周聚合规则
- [ ] 写 reflection 回写校验规则

### Phase E：Mapper 和 Service 设计文档
- [ ] 定义每个 Mapper 的职责
- [ ] 定义每个 Service 的职责
- [ ] 定义事务边界
- [ ] 定义每个接口会写哪些表
- [ ] 定义哪些查询必须自定义 SQL

### Phase F：编码任务拆解文档
- [ ] 把 Flyway 工作拆成 migration 任务
- [ ] 按接口组拆 DTO/controller 任务
- [ ] 按表拆 mapper 任务
- [ ] 按业务域拆 service 任务
- [ ] 按层拆测试任务（controller/service/domain/migration）

---

## 14. 接下来 6 份 java-core 文档的推荐顺序

1. `java-core-db-schema.md`
2. `java-core-api-contracts.md`
3. `java-core-domain-rules.md`
4. `java-core-package-design.md`
5. `java-core-task-breakdown.md`
6. `java-core-test-strategy.md`

这个顺序是刻意安排的：
- 先定义数据真相
- 再定义接口契约
- 再定义领域规则
- 再定义模块结构
- 再定义实施任务
- 最后定义测试策略

---

## 15. 当前最建议做的事

**先不要写 Java 代码。**

先做这几件事：
1. 确认这 10 张表是否全部保留
2. 确认 `daily_plan_snapshot` 和 `daily_plan_item` 留在 MVP 中
3. 确认接口 1~18 的范围
4. 继续把上面列出的 java-core 文档集补齐

等这些稳定后，再开始实现，会顺很多。
