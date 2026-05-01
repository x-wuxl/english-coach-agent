# Java Core API 契约设计

日期：2026-03-25  
状态：draft  
适用范围：`english-coach-agent/java-core`

---

## 1. 文档目标

这份文档用于正式定义 `java-core` 的 API 契约。

目标是提前锁定：
- 接口路径
- 请求方法
- 请求参数
- 请求 DTO 字段
- 响应 DTO 字段
- 分页结构
- 统一响应包装
- 错误码约定
- 幂等性约定
- 副作用说明
- 对应读写的数据表

这份文档是后续编写：
- Controller
- Request DTO
- Response DTO
- Application Service
- Mapper 查询

的直接依据。

---

## 2. 总体 API 设计原则

### 2.1 接口定位
`java-core` API 面向两类调用方：
1. `python-agent`
2. 后续 Web / 管理端 / 调试工具

因此契约必须满足：
- 结构稳定
- 字段明确
- 可解释
- 可长期演进

### 2.2 路径风格
统一挂在 `/api` 下。

建议按业务域划分：
- `/api/users`
- `/api/placement`
- `/api/content`
- `/api/plans`
- `/api/sessions`
- `/api/mastery`
- `/api/memory`
- `/api/reviews`

### 2.3 响应风格
所有接口统一使用响应包装。

成功响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

失败响应：
```json
{
  "code": 4001,
  "message": "invalid request",
  "data": null
}
```

### 2.4 时间与日期格式
统一约定：
- `LocalDate` → `yyyy-MM-dd`
- `LocalDateTime` → `yyyy-MM-dd'T'HH:mm:ss`
- 纯时间字段 → `HH:mm`

### 2.5 枚举字段返回方式
第一版统一使用字符串返回，不返回数值枚举。

例如：
- `GENERAL`
- `DAILY_LEARNING`
- `WEAK_OUTPUT`

这样更直观，也更方便 Python 侧解析。

---

## 3. 通用响应结构

## 3.1 BaseResponse

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

字段定义：
- `code`：业务响应码，`0` 表示成功
- `message`：响应说明
- `data`：实际业务数据

### Java DTO 建议
- `BaseResponse<T>`

字段：
- `Integer code`
- `String message`
- `T data`

---

## 3.2 分页响应结构

用于列表接口。

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [],
    "page": 1,
    "size": 20,
    "total": 135,
    "totalPages": 7
  }
}
```

### Java DTO 建议
- `PageResponse<T>`

字段：
- `List<T> items`
- `Integer page`
- `Integer size`
- `Long total`
- `Integer totalPages`

---

## 4. 错误码约定

### 4.1 通用错误码
- `0`：成功
- `4001`：请求参数非法
- `4002`：枚举值非法
- `4003`：日期/时间格式非法
- `4004`：分页参数非法
- `4010`：未授权（预留）
- `4030`：无权限（预留）
- `4041`：用户不存在
- `4042`：学习会话不存在
- `4043`：日计划不存在
- `4044`：学习项不存在
- `4045`：周复盘不存在
- `4091`：用户编码重复
- `4092`：同日计划重复生成
- `4093`：会话已完成，不允许重复完成
- `4094`：定级结果不允许空提交
- `5001`：内部领域逻辑错误
- `5002`：数据持久化错误

### 4.2 错误响应示例
```json
{
  "code": 4041,
  "message": "user not found",
  "data": null
}
```

---

## 5. 分页与查询参数约定

### 5.1 分页参数
通用约定：
- `page`：从 `1` 开始
- `size`：默认 `20`
- `size` 最大建议 `100`

### 5.2 空结果处理
- 查询列表为空时，返回成功，`items=[]`
- 查询单对象不存在时，返回 `404x`

### 5.3 排序策略
第一版尽量在接口内部固定排序，不开放复杂排序参数，避免契约太早复杂化。

---

## 6. API 契约详解

## 6.1 用户画像接口

### 6.1.1 创建用户画像
**Method**：`POST /api/users`

**用途**：
创建新的学习用户画像。

**请求 DTO**：`CreateUserProfileRequest`

字段：
- `userCode: String`，必填，最大 64
- `goal: String`，必填，枚举 `GoalType`
- `subGoals: List<String>`，可选
- `dailyMinutes: Integer`，必填，5~180
- `studyStartTime: String`，可选，格式 `HH:mm`
- `reviewTime: String`，可选，格式 `HH:mm`
- `preferredModes: List<String>`，可选
- `motivationStyle: String`，可选
- `fatigueTolerance: String`，可选

**请求示例**：
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

**响应 DTO**：`UserProfileResponse`

字段：
- `id: Long`
- `userCode: String`
- `goal: String`
- `subGoals: List<String>`
- `dailyMinutes: Integer`
- `studyStartTime: String`
- `reviewTime: String`
- `overallLevel: String`
- `vocabLevel: String`
- `grammarLevel: String`
- `readingLevel: String`
- `outputLevel: String`
- `preferredModes: List<String>`
- `motivationStyle: String`
- `fatigueTolerance: String`
- `status: String`
- `createdAt: String`
- `updatedAt: String`

**副作用**：
- 写入 `user_profile`

**错误码**：
- `4001`
- `4002`
- `4003`
- `4091`

---

### 6.1.2 查询用户画像
**Method**：`GET /api/users/{userId}`

**用途**：
获取用户基础画像和当前等级摘要。

**Path 参数**：
- `userId: Long`，必填

**响应 DTO**：`UserProfileResponse`

**读表**：
- `user_profile`

**错误码**：
- `4041`

---

### 6.1.3 更新用户画像
**Method**：`PUT /api/users/{userId}`

**用途**：
更新用户学习偏好、时间设置和部分画像字段。

**请求 DTO**：`UpdateUserProfileRequest`

可更新字段：
- `goal`
- `subGoals`
- `dailyMinutes`
- `studyStartTime`
- `reviewTime`
- `preferredModes`
- `motivationStyle`
- `fatigueTolerance`

**响应 DTO**：`UserProfileResponse`

**写表**：
- `user_profile`

**错误码**：
- `4001`
- `4002`
- `4003`
- `4041`

---

## 6.2 定级接口

### 6.2.1 提交定级评估
**Method**：`POST /api/placement/assess`

**用途**：
根据结构化答题结果计算用户初始能力画像。

**请求 DTO**：`PlacementAssessmentRequest`

字段：
- `userId: Long`，必填
- `answers: List<PlacementAnswerRequest>`，必填，不能为空

`PlacementAnswerRequest` 字段：
- `section: String`，必填，例如 `vocab / grammar / reading / output`
- `questionId: String`，必填
- `result: String`，必填，枚举 `AttemptResult`
- `responseText: String`，可选
- `responseTimeMs: Integer`，可选
- `hintUsed: Boolean`，可选

**请求示例**：
```json
{
  "userId": 1,
  "answers": [
    {
      "section": "vocab",
      "questionId": "v_001",
      "result": "CORRECT",
      "responseText": "book",
      "responseTimeMs": 3200,
      "hintUsed": false
    }
  ]
}
```

**响应 DTO**：`PlacementAssessmentResponse`

字段：
- `overallLevel: String`
- `vocabLevel: String`
- `grammarLevel: String`
- `readingLevel: String`
- `outputLevel: String`
- `weaknesses: List<String>`
- `suggestedDailyRhythm: SuggestedDailyRhythmResponse`

`SuggestedDailyRhythmResponse` 字段：
- `newItems: Integer`
- `reviewItems: Integer`
- `outputTasks: Integer`

**副作用**：
- 更新 `user_profile`
- 写入 `study_session`（`PLACEMENT` 类型）

**写表**：
- `user_profile`
- `study_session`

**错误码**：
- `4001`
- `4002`
- `4041`
- `4094`

---

## 6.3 内容接口

### 6.3.1 查询学习项列表
**Method**：`GET /api/content/items`

**用途**：
查询种子学习项列表。

**Query 参数**：
- `type: String`，可选
- `theme: String`，可选
- `difficultyMin: Integer`，可选
- `difficultyMax: Integer`，可选
- `page: Integer`，默认 1
- `size: Integer`，默认 20

**响应 DTO**：`PageResponse<LearningItemSummaryResponse>`

`LearningItemSummaryResponse` 字段：
- `id: Long`
- `itemCode: String`
- `type: String`
- `content: String`
- `meaningZh: String`
- `difficulty: Integer`
- `theme: String`
- `status: String`

**读表**：
- `learning_item`

**错误码**：
- `4001`
- `4004`

---

### 6.3.2 查询学习项详情
**Method**：`GET /api/content/items/{itemId}`

**用途**：
查询单个学习项详情。

**响应 DTO**：`LearningItemDetailResponse`

字段：
- `id`
- `itemCode`
- `type`
- `content`
- `meaningZh`
- `difficulty`
- `theme`
- `tags: List<String>`
- `examples: List<ExampleResponse>`
- `relatedItemCodes: List<String>`
- `status`
- `createdAt`
- `updatedAt`

**读表**：
- `learning_item`

**错误码**：
- `4044`

---

## 6.4 日计划接口

### 6.4.1 生成日计划
**Method**：`POST /api/plans/daily:generate`

**用途**：
计算并持久化一份日计划。

**请求 DTO**：`GenerateDailyPlanRequest`

字段：
- `userId: Long`，必填
- `planDate: String`，必填，`yyyy-MM-dd`
- `planType: String`，必填，`NORMAL / LIGHT_REVIEW`

**响应 DTO**：`DailyPlanResponse`

字段：
- `planCode: String`
- `planDate: String`
- `planType: String`
- `status: String`
- `newItems: List<DailyPlanItemResponse>`
- `reviewItems: List<DailyPlanItemResponse>`
- `outputTasks: List<OutputTaskResponse>`
- `rationale: DailyPlanRationaleResponse`

`DailyPlanItemResponse` 字段：
- `itemId: Long`
- `content: String`
- `itemRole: String`
- `recommendedMode: String`
- `priorityScore: BigDecimal`
- `selectionReason: Map<String, Object>`

`OutputTaskResponse` 字段：
- `taskType: String`
- `relatedItemIds: List<Long>`

`DailyPlanRationaleResponse` 字段：
- `loadDecision: String`
- `whyReviewThese: List<String>`
- `whyNewCountIsThis: String`

**副作用**：
- 写入 `daily_plan_snapshot`
- 写入 `daily_plan_item`

**读表**：
- `user_profile`
- `mastery_state`
- `learning_item`
- `attempt_log`（视策略可选）

**写表**：
- `daily_plan_snapshot`
- `daily_plan_item`

**错误码**：
- `4001`
- `4002`
- `4003`
- `4041`
- `4092`

**幂等性建议**：
- 对同一 `userId + planDate + planType`，第一版建议禁止重复生成
- 若要重生成，后续可单独设计 refresh 接口

---

### 6.4.2 查询日计划
**Method**：`GET /api/plans/daily`

**用途**：
按用户和日期获取已生成的日计划。

**Query 参数**：
- `userId: Long`，必填
- `planDate: String`，必填
- `planType: String`，可选，默认 `NORMAL`

**响应 DTO**：`DailyPlanResponse`

**读表**：
- `daily_plan_snapshot`
- `daily_plan_item`
- `learning_item`

**错误码**：
- `4001`
- `4003`
- `4043`

---

## 6.5 学习会话接口

### 6.5.1 开始学习会话
**Method**：`POST /api/sessions/start`

**用途**：
在正式学习前创建 session 记录。

**请求 DTO**：`StartStudySessionRequest`

字段：
- `userId: Long`，必填
- `sessionType: String`，必填
- `planCode: String`，可选
- `focusTheme: String`，可选

**响应 DTO**：`StudySessionStartResponse`

字段：
- `sessionId: Long`
- `sessionCode: String`
- `sessionType: String`
- `status: String`
- `startedAt: String`

**写表**：
- `study_session`

**错误码**：
- `4001`
- `4002`
- `4041`
- `4043`（如果带 planCode 且找不到计划）

---

### 6.5.2 批量提交尝试记录
**Method**：`POST /api/sessions/{sessionId}/attempts`

**用途**：
批量写入结构化答题记录，并推动掌握状态更新。

**请求 DTO**：`SubmitAttemptsRequest`

字段：
- `attempts: List<AttemptSubmitItemRequest>`，必填，不能为空

`AttemptSubmitItemRequest` 字段：
- `learningItemId: Long`，必填
- `mode: String`，必填
- `result: String`，必填
- `responseText: String`，可选
- `responseTimeMs: Integer`，可选
- `hintUsed: Boolean`，可选
- `errorType: String`，可选
- `errorDetails: Map<String, Object>`，可选

**响应 DTO**：`SubmitAttemptsResponse`

字段：
- `sessionId: Long`
- `acceptedCount: Integer`
- `updatedMasteryItemCount: Integer`

**副作用**：
- 写入 `attempt_log`
- 更新 `mastery_state`

**读表**：
- `study_session`
- `learning_item`
- `mastery_state`

**写表**：
- `attempt_log`
- `mastery_state`

**错误码**：
- `4001`
- `4002`
- `4042`
- `4044`
- `4093`

---

### 6.5.3 完成学习会话
**Method**：`POST /api/sessions/{sessionId}/complete`

**用途**：
结束学习会话并汇总统计结果。

**请求 DTO**：`CompleteStudySessionRequest`

字段：
- `durationMin: Integer`，可选
- `fatigueFeedback: String`，可选
- `moodFeedback: String`，可选

**响应 DTO**：`StudySessionCompleteResponse`

字段：
- `sessionId: Long`
- `status: String`
- `accuracy: BigDecimal`
- `completionRate: BigDecimal`
- `newItemsCount: Integer`
- `reviewItemsCount: Integer`
- `completedAt: String`

**副作用**：
- 更新 `study_session`

**读表**：
- `study_session`
- `attempt_log`

**写表**：
- `study_session`

**错误码**：
- `4001`
- `4042`
- `4093`

---

### 6.5.4 查询会话详情
**Method**：`GET /api/sessions/{sessionId}`

**用途**：
查询会话摘要和 attempt 明细。

**响应 DTO**：`StudySessionDetailResponse`

字段：
- `sessionId`
- `sessionCode`
- `sessionType`
- `status`
- `sessionDate`
- `durationMin`
- `accuracy`
- `completionRate`
- `fatigueFeedback`
- `moodFeedback`
- `focusTheme`
- `attempts: List<AttemptLogResponse>`

`AttemptLogResponse` 字段：
- `attemptId: Long`
- `learningItemId: Long`
- `mode: String`
- `result: String`
- `responseText: String`
- `responseTimeMs: Integer`
- `hintUsed: Boolean`
- `errorType: String`
- `errorDetails: Map<String, Object>`
- `occurredAt: String`

**读表**：
- `study_session`
- `attempt_log`

**错误码**：
- `4042`

---

## 6.6 掌握状态接口

### 6.6.1 查询掌握状态列表
**Method**：`GET /api/mastery`

**用途**：
按用户查询掌握状态，供调试、Agent 上下文和内部管理使用。

**Query 参数**：
- `userId: Long`，必填
- `status: String`，可选
- `dueBefore: String`，可选，`yyyy-MM-dd'T'HH:mm:ss`
- `theme: String`，可选
- `page: Integer`，默认 1
- `size: Integer`，默认 20

**响应 DTO**：`PageResponse<MasteryStateResponse>`

`MasteryStateResponse` 字段：
- `id`
- `learningItemId`
- `itemContent`
- `itemType`
- `theme`
- `seenCount`
- `correctCount`
- `wrongCount`
- `correctStreak`
- `recognitionScore`
- `recallScore`
- `outputScore`
- `memoryStrength`
- `forgetRisk`
- `lastSeenAt`
- `nextReviewAt`
- `lastMode`
- `recommendedMode`
- `status`

**读表**：
- `mastery_state`
- `learning_item`

**错误码**：
- `4001`
- `4002`
- `4003`
- `4004`
- `4041`

---

### 6.6.2 查询当前到期复习项
**Method**：`GET /api/mastery/due-review`

**用途**：
返回当前应复习的学习项，按优先级排序。

**Query 参数**：
- `userId: Long`，必填
- `at: String`，可选，默认当前时间
- `limit: Integer`，默认 20，最大 100

**响应 DTO**：`DueReviewItemResponse`

字段：
- `learningItemId: Long`
- `itemContent: String`
- `itemType: String`
- `theme: String`
- `priorityScore: BigDecimal`
- `forgetRisk: BigDecimal`
- `nextReviewAt: String`
- `recommendedMode: String`
- `status: String`

**读表**：
- `mastery_state`
- `learning_item`

**错误码**：
- `4001`
- `4003`
- `4041`

---

## 6.7 记忆回写接口

### 6.7.1 保存会话反思
**Method**：`POST /api/memory/session-reflections`

**用途**：
接收 Python 侧的结构化会话反思并持久化。

**请求 DTO**：`SaveSessionReflectionRequest`

字段：
- `sessionId: Long`，必填
- `userId: Long`，必填
- `detectedPatterns: List<String>`，可选
- `fatigueSignal: String`，可选
- `recommendedAdjustment: String`，可选
- `coachNotes: String`，可选
- `confidence: BigDecimal`，可选
- `source: String`，必填，默认 `python-agent`

**响应 DTO**：`SessionReflectionResponse`

字段：
- `id: Long`
- `sessionId: Long`
- `userId: Long`
- `detectedPatterns: List<String>`
- `fatigueSignal: String`
- `recommendedAdjustment: String`
- `coachNotes: String`
- `confidence: BigDecimal`
- `source: String`
- `createdAt: String`

**写表**：
- `session_reflection`

**错误码**：
- `4001`
- `4041`
- `4042`

---

### 6.7.2 保存行为模式摘要
**Method**：`POST /api/memory/behavior-patterns`

**用途**：
持久化 Python 生成的滚动行为摘要。

**请求 DTO**：`SaveBehaviorPatternSummaryRequest`

字段：
- `userId: Long`，必填
- `summaryType: String`，必填
- `patterns: List<String>`，可选
- `recommendedStrategy: Map<String, Object>`，可选
- `confidence: BigDecimal`，可选
- `source: String`，必填

**响应 DTO**：`BehaviorPatternSummaryResponse`

字段：
- `id: Long`
- `userId: Long`
- `summaryType: String`
- `patterns: List<String>`
- `recommendedStrategy: Map<String, Object>`
- `confidence: BigDecimal`
- `source: String`
- `createdAt: String`

**写表**：
- `behavior_pattern_summary`

**错误码**：
- `4001`
- `4041`

---

## 6.8 周复盘接口

### 6.8.1 生成周复盘
**Method**：`POST /api/reviews/weekly:generate`

**用途**：
聚合指定一周的学习行为，生成并持久化周复盘结果。

**请求 DTO**：`GenerateWeeklyReviewRequest`

字段：
- `userId: Long`，必填
- `weekStartDate: String`，必填
- `weekEndDate: String`，必填

**响应 DTO**：`WeeklyReviewResponse`

字段：
- `userId: Long`
- `weekStartDate: String`
- `weekEndDate: String`
- `completionRate: BigDecimal`
- `studyMinutes: Integer`
- `newItemsCount: Integer`
- `reviewItemsCount: Integer`
- `highFrequencyErrorTypes: List<String>`
- `strongestThemes: List<String>`
- `weakestThemes: List<String>`
- `nextWeekSuggestion: Map<String, Object>`
- `createdAt: String`

**副作用**：
- 写入 `weekly_review_snapshot`

**读表**：
- `study_session`
- `attempt_log`
- `mastery_state`
- `learning_item`

**写表**：
- `weekly_review_snapshot`

**错误码**：
- `4001`
- `4003`
- `4041`

---

### 6.8.2 查询周复盘快照
**Method**：`GET /api/reviews/weekly`

**用途**：
查询已生成的周复盘快照。

**Query 参数**：
- `userId: Long`，必填
- `weekStartDate: String`，必填
- `weekEndDate: String`，必填

**响应 DTO**：`WeeklyReviewResponse`

**读表**：
- `weekly_review_snapshot`

**错误码**：
- `4001`
- `4003`
- `4045`

---

## 7. 接口与数据表映射总览

| 接口 | 读表 | 写表 |
|---|---|---|
| POST /api/users | - | user_profile |
| GET /api/users/{userId} | user_profile | - |
| PUT /api/users/{userId} | user_profile | user_profile |
| POST /api/placement/assess | user_profile | user_profile, study_session |
| GET /api/content/items | learning_item | - |
| GET /api/content/items/{itemId} | learning_item | - |
| POST /api/plans/daily:generate | user_profile, mastery_state, learning_item | daily_plan_snapshot, daily_plan_item |
| GET /api/plans/daily | daily_plan_snapshot, daily_plan_item, learning_item | - |
| POST /api/sessions/start | study_session, daily_plan_snapshot(可选) | study_session |
| POST /api/sessions/{sessionId}/attempts | study_session, learning_item, mastery_state | attempt_log, mastery_state |
| POST /api/sessions/{sessionId}/complete | study_session, attempt_log | study_session |
| GET /api/sessions/{sessionId} | study_session, attempt_log | - |
| GET /api/mastery | mastery_state, learning_item | - |
| GET /api/mastery/due-review | mastery_state, learning_item | - |
| POST /api/memory/session-reflections | study_session | session_reflection |
| POST /api/memory/behavior-patterns | user_profile | behavior_pattern_summary |
| POST /api/reviews/weekly:generate | study_session, attempt_log, mastery_state, learning_item | weekly_review_snapshot |
| GET /api/reviews/weekly | weekly_review_snapshot | - |

---

## 8. 幂等性约定

### 建议幂等处理
- `POST /api/users`：不天然幂等，靠 `user_code` 唯一约束兜底
- `POST /api/placement/assess`：第一版可视为非幂等，但同一轮评估不建议重复提交
- `POST /api/plans/daily:generate`：建议限制同一用户同一日期同一 planType 只能生成一次
- `POST /api/sessions/start`：非幂等
- `POST /api/sessions/{sessionId}/attempts`：默认非幂等，后续可增加 attemptCode 去重策略
- `POST /api/sessions/{sessionId}/complete`：需要防重复完成
- `POST /api/reviews/weekly:generate`：建议按 user + week 唯一约束控制重复生成

---

## 9. 当前结论

当前 `java-core` 第一版建议正式包含 18 个接口：

1. 创建用户画像
2. 查询用户画像
3. 更新用户画像
4. 提交定级评估
5. 查询学习项列表
6. 查询学习项详情
7. 生成日计划
8. 查询日计划
9. 开始学习会话
10. 批量提交尝试记录
11. 完成学习会话
12. 查询会话详情
13. 查询掌握状态列表
14. 查询当前到期复习项
15. 保存会话反思
16. 保存行为模式摘要
17. 生成周复盘
18. 查询周复盘快照

这 18 个接口已经足够覆盖 `java-core` 的 MVP 主链路。

---

## 10. 下一步建议

在 API 契约确认后，下一份最值得写的是：

**`java-core-domain-rules.md`**

因为接下来真正会影响代码实现质量的，就是：
- 定级评分规则
- 日负载规则
- 复习优先级公式
- 掌握状态迁移规则
- 周复盘聚合规则

接口现在只是壳，规则才是骨头。
