# Java Core 任务拆解文档

日期：2026-03-26  
状态：draft  
适用范围：`english-coach-agent/java-core`

---

## 1. 文档目标

这份文档不是继续讲架构，也不是写代码。

它的作用是把已经确定的三份核心设计文档：
- `java-core-db-schema.md`
- `java-core-api-contracts.md`
- `java-core-domain-rules.md`

进一步翻译成一份真正可执行的开发任务拆解。

这份文档重点回答：
- 先做什么
- 后做什么
- 每一步依赖什么
- 每一步要产出哪些类/文件
- 每一步完成标准是什么
- 哪些测试应该在这一阶段完成

目标是让 `java-core` 的实现顺序稳定下来，避免边写边飘。

---

## 2. 拆解原则

### 2.1 先底座，后业务
先把：
- 工程配置
- migration
- 通用结构
- 枚举和响应体

做稳，再进入业务域。

### 2.2 先核心主链路，后辅助能力
优先实现：
- 用户画像
- 定级
- 日计划
- 会话
- 掌握状态
- 周复盘

记忆回写、内容查询这种辅助能力可以靠后。

### 2.3 先规则真相，后表现包装
核心状态更新逻辑必须先落地。
不要先写一堆 Controller 壳子，再回头补规则。

### 2.4 一次只做一个清晰的交付物
每个阶段要有明确产出。
不要同时并行写太多模块，不然很容易相互污染。

---

## 3. 总体实施阶段

建议将 `java-core` 拆成 9 个阶段：

1. Phase 0：工程基础与通用骨架
2. Phase 1：数据库 schema 与 migration
3. Phase 2：用户画像模块
4. Phase 3：定级模块
5. Phase 4：学习内容模块
6. Phase 5：日计划与复习调度模块
7. Phase 6：学习会话与掌握状态模块
8. Phase 7：周复盘模块
9. Phase 8：记忆回写模块

---

## 4. Phase 0：工程基础与通用骨架

### 4.1 目标
建立 `java-core` 可持续开发的基础骨架。

### 4.2 前置条件
- `pom.xml` 已存在
- Spring Boot 可以正常启动
- Maven 测试可运行

### 4.3 产出内容

#### 目录结构
- `api/`
- `application/`
- `domain/`
- `infrastructure/persistence/`
- `common/enums/`
- `common/response/`
- `common/exception/`
- `common/config/`

#### 通用类
- `BaseResponse<T>`
- `PageResponse<T>`
- `BusinessException`
- `GlobalExceptionHandler`
- 基础枚举类

### 4.4 具体任务

#### Task 0-1：建立基础包结构
产出：
- 空 package 目录
- package-info（可选）

#### Task 0-2：定义统一响应结构
产出：
- `BaseResponse`
- `PageResponse`

#### Task 0-3：定义统一异常体系
产出：
- `BusinessException`
- `ErrorCodeEnum`
- `GlobalExceptionHandler`

#### Task 0-4：定义核心枚举
至少包含：
- `GoalType`
- `LearningItemType`
- `MasteryStatus`
- `SessionType`
- `SessionStatus`
- `AttemptResult`
- `ErrorType`
- `PlanType`
- `PlanStatus`
- `ItemRole`

### 4.5 验收标准
- 项目结构清晰可用
- 能返回统一响应格式
- 参数异常和业务异常能统一落成标准错误响应
- 枚举范围与文档一致

### 4.6 推荐测试
- `GlobalExceptionHandlerTest`
- `BaseResponseSerializationTest`
- 枚举值映射测试

---

## 5. Phase 1：数据库 schema 与 migration

### 5.1 目标
把 10 张表正式落成 migration，并保证测试库可正常初始化。

### 5.2 依赖
- `java-core-db-schema.md`
- Phase 0 完成

### 5.3 产出内容

#### Migration 文件
- `V1__init_core_tables.sql`
- `V2__init_plan_and_review_tables.sql`
- `V3__init_memory_tables.sql`
- `V4__seed_learning_items.sql`

#### 测试
- Flyway migration 测试
- Testcontainers PostgreSQL 初始化测试

### 5.4 具体任务

#### Task 1-1：实现核心主表 migration
表：
- `user_profile`
- `learning_item`
- `mastery_state`
- `study_session`
- `attempt_log`

#### Task 1-2：实现计划与复盘相关 migration
表：
- `daily_plan_snapshot`
- `daily_plan_item`
- `weekly_review_snapshot`

#### Task 1-3：实现记忆回写相关 migration
表：
- `session_reflection`
- `behavior_pattern_summary`

#### Task 1-4：实现 seed 数据 migration
- MVP 最小 learning items

#### Task 1-5：实现 migration 测试
验证：
- 表存在
- 约束存在
- 唯一索引存在
- seed 数据存在

### 5.5 验收标准
- 测试库能完整执行全部 migration
- 10 张表全部建成
- 关键唯一约束和索引存在
- seed 数据可查询

### 5.6 推荐测试
- `FlywayMigrationTest`
- `SeedDataLoadTest`

---

## 6. Phase 2：用户画像模块

### 6.1 目标
完成用户画像的创建、查询、更新。

### 6.2 依赖
- Phase 1 完成
- `user_profile` 表可用
- API 契约已确认

### 6.3 产出内容

#### DTO
- `CreateUserProfileRequest`
- `UpdateUserProfileRequest`
- `UserProfileResponse`

#### Controller
- `UserProfileController`

#### Service
- `UserProfileService`

#### Persistence
- `UserProfileDO`
- `UserProfileMapper`

### 6.4 具体任务

#### Task 2-1：定义 DTO
对齐 API 文档字段。

#### Task 2-2：定义 DO 和 Mapper
包含：
- insert
- selectById
- selectByUserCode
- update

#### Task 2-3：实现 Service
包含：
- createProfile
- getProfile
- updateProfile

#### Task 2-4：实现 Controller
接口：
- `POST /api/users`
- `GET /api/users/{userId}`
- `PUT /api/users/{userId}`

#### Task 2-5：补齐参数校验与重复 userCode 处理

### 6.5 验收标准
- 用户可创建
- 可按 id 查询
- 可更新指定字段
- 重复 `user_code` 能正确报错
- 返回结构符合统一响应包装

### 6.6 推荐测试
- `UserProfileControllerTest`
- `UserProfileServiceTest`
- `UserProfileMapperTest`

---

## 7. Phase 3：定级模块

### 7.1 目标
实现结构化定级评分和用户等级回写。

### 7.2 依赖
- 用户模块完成
- `study_session` 表可用
- `java-core-domain-rules.md` 中定级规则已确认

### 7.3 产出内容

#### DTO
- `PlacementAssessmentRequest`
- `PlacementAnswerRequest`
- `PlacementAssessmentResponse`

#### Domain
- `PlacementScorer`
- `PlacementLevelMapper`（或同类规则类）

#### Service
- `PlacementService`

#### Controller
- `PlacementController`

### 7.4 具体任务

#### Task 3-1：实现定级领域规则类
包含：
- section score
- overall score
- weaknesses 提取
- suggestedDailyRhythm 计算

#### Task 3-2：实现 PlacementService
职责：
- 校验输入
- 调用 scorer
- 回写 `user_profile` level 字段
- 写 placement 类型 session

#### Task 3-3：实现 PlacementController
接口：
- `POST /api/placement/assess`

### 7.5 验收标准
- 不同 section 表现能映射到预期 level
- `weaknesses` 和 `suggestedDailyRhythm` 返回合理
- 用户 level 字段能回写成功
- placement session 被创建

### 7.6 推荐测试
- `PlacementScorerTest`
- `PlacementServiceTest`
- `PlacementControllerTest`

---

## 8. Phase 4：学习内容模块

### 8.1 目标
实现种子学习项查询能力，支撑计划和调试。

### 8.2 依赖
- `learning_item` 表已有 seed 数据

### 8.3 产出内容

#### DTO
- `LearningItemSummaryResponse`
- `LearningItemDetailResponse`
- `ExampleResponse`

#### Service
- `ContentService`

#### Controller
- `ContentController`

#### Persistence
- `LearningItemDO`
- `LearningItemMapper`

### 8.4 具体任务

#### Task 4-1：实现 Mapper 查询
支持：
- 列表条件查询
- 详情查询

#### Task 4-2：实现 ContentService

#### Task 4-3：实现 ContentController
接口：
- `GET /api/content/items`
- `GET /api/content/items/{itemId}`

### 8.5 验收标准
- 支持按 type/theme/difficulty 查询
- 详情能返回 examples/tags/related items
- 分页结构稳定

### 8.6 推荐测试
- `ContentControllerTest`
- `ContentServiceTest`
- `LearningItemMapperTest`

---

## 9. Phase 5：日计划与复习调度模块

### 9.1 目标
实现系统最核心的决策能力之一：
- 计算今日负载
- 选复习项
- 选新学项
- 组装并持久化日计划

### 9.2 依赖
- 用户画像模块完成
- 内容模块完成
- `mastery_state` 表可用
- `daily_plan_snapshot` / `daily_plan_item` 表可用
- 定级结果已可写入用户画像

### 9.3 产出内容

#### DTO
- `GenerateDailyPlanRequest`
- `DailyPlanResponse`
- `DailyPlanItemResponse`
- `OutputTaskResponse`
- `DailyPlanRationaleResponse`

#### Domain
- `DailyLoadPolicy`
- `ReviewPriorityCalculator`
- `NewItemSelectionPolicy`
- `PlanAssemblyPolicy`

#### Service
- `DailyPlanService`
- `MemorySchedulerService`

#### Controller
- `DailyPlanController`

#### Persistence
- `DailyPlanSnapshotDO`
- `DailyPlanItemDO`
- `DailyPlanSnapshotMapper`
- `DailyPlanItemMapper`
- `MasteryStateMapper` 自定义优先级查询

### 9.4 具体任务

#### Task 5-1：实现日负载规则类
输入：
- daily_minutes
- recent completion rate
- recent accuracy
- fatigue
- overdue volume

输出：
- new/review/output count
- load decision

#### Task 5-2：实现复习优先级规则类
实现 priority 公式和排序。

#### Task 5-3：实现新学项选择规则
按：
- goal
- theme continuity
- difficulty proximity
- expression usefulness

筛选新项。

#### Task 5-4：实现日计划装配逻辑
产出：
- newItems
- reviewItems
- outputTasks
- rationale

#### Task 5-5：实现 persistence
- 存 plan snapshot
- 存 plan items
- 按 user/date 查询计划

#### Task 5-6：实现 Controller
接口：
- `POST /api/plans/daily:generate`
- `GET /api/plans/daily`

### 9.5 验收标准
- 不同状态用户能生成不同负载日计划
- overdue + weak_output 项优先级明显更高
- 同一 `userId + planDate + planType` 不能重复生成
- rationale 字段能解释为什么这么排

### 9.6 推荐测试
- `DailyLoadPolicyTest`
- `ReviewPriorityCalculatorTest`
- `DailyPlanServiceTest`
- `DailyPlanControllerTest`
- `DailyPlanMapperTest`

---

## 10. Phase 6：学习会话与掌握状态模块

### 10.1 目标
实现完整学习执行主链路：
- start session
- submit attempts
- update mastery
- complete session
- query session detail

这是 `java-core` 中最复杂的一段。

### 10.2 依赖
- 日计划模块完成
- `attempt_log`、`study_session`、`mastery_state` 已可用
- 领域规则中状态迁移规则已确认

### 10.3 产出内容

#### DTO
- `StartStudySessionRequest`
- `StudySessionStartResponse`
- `SubmitAttemptsRequest`
- `AttemptSubmitItemRequest`
- `SubmitAttemptsResponse`
- `CompleteStudySessionRequest`
- `StudySessionCompleteResponse`
- `StudySessionDetailResponse`
- `AttemptLogResponse`
- `MasteryStateResponse`
- `DueReviewItemResponse`

#### Domain
- `MasteryStateMachine`
- `ScoreUpdatePolicy`
- `NextReviewPolicy`
- `RecommendedModePolicy`
- `ErrorTypeNormalizer`

#### Service
- `StudySessionService`
- `MasteryStateService`
- `FeedbackAnalysisService`

#### Controller
- `StudySessionController`
- `MasteryController`

#### Persistence
- `StudySessionDO`
- `AttemptLogDO`
- `MasteryStateDO`
- `StudySessionMapper`
- `AttemptLogMapper`
- `MasteryStateMapper`

### 10.4 具体任务

#### Task 6-1：实现开始会话接口
接口：
- `POST /api/sessions/start`

#### Task 6-2：实现错误类型归一化规则
统一 normalize 所有错误类型。

#### Task 6-3：实现分数更新规则
更新：
- recognition_score
- recall_score
- output_score
- forget_risk
- correct_streak

#### Task 6-4：实现状态迁移规则
实现：
- `NEW -> LEARNING`
- `LEARNING -> REVIEWING`
- `-> WEAK_OUTPUT`
- `-> MASTERED`
- 已掌握项回退逻辑

#### Task 6-5：实现批量提交 attempts
接口：
- `POST /api/sessions/{sessionId}/attempts`

需要：
- 批量写入 `attempt_log`
- 更新 `mastery_state`
- 正确维护 `next_review_at`
- 更新 `recommended_mode`

#### Task 6-6：实现完成会话逻辑
接口：
- `POST /api/sessions/{sessionId}/complete`

需要：
- 统计 accuracy
- 统计 completionRate
- 统计本次新学/复习数量

#### Task 6-7：实现会话详情查询
接口：
- `GET /api/sessions/{sessionId}`

#### Task 6-8：实现掌握状态查询接口
接口：
- `GET /api/mastery`
- `GET /api/mastery/due-review`

### 10.5 验收标准
- session 可正常开始和完成
- attempts 能正确入库
- mastery_state 会按规则变化
- weak_output 能被正确识别
- due-review 查询能按优先级返回候选项

### 10.6 推荐测试
- `MasteryStateMachineTest`
- `ScoreUpdatePolicyTest`
- `NextReviewPolicyTest`
- `StudySessionServiceTest`
- `StudySessionControllerTest`
- `MasteryControllerTest`

---

## 11. Phase 7：周复盘模块

### 11.1 目标
实现一周行为聚合与周复盘快照生成。

### 11.2 依赖
- session 模块完成
- attempt_log 和 mastery_state 数据可用
- 周复盘聚合规则已确认

### 11.3 产出内容

#### DTO
- `GenerateWeeklyReviewRequest`
- `WeeklyReviewResponse`

#### Domain
- `WeeklyReviewAggregator`
- `ThemePerformanceCalculator`
- `NextWeekSuggestionPolicy`

#### Service
- `WeeklyReviewService`

#### Controller
- `WeeklyReviewController`

#### Persistence
- `WeeklyReviewSnapshotDO`
- `WeeklyReviewSnapshotMapper`

### 11.4 具体任务

#### Task 7-1：实现一周统计聚合规则
聚合：
- completion rate
- study minutes
- new/review counts
- 高频错误类型

#### Task 7-2：实现 strongest / weakest theme 计算

#### Task 7-3：实现 next week suggestion 规则
输出：
- load
- outputRatio
- reviewFocus

#### Task 7-4：实现周复盘快照 persistence

#### Task 7-5：实现 Controller
接口：
- `POST /api/reviews/weekly:generate`
- `GET /api/reviews/weekly`

### 11.5 验收标准
- 能按一周范围聚合数据
- 高频错误类型统计正确
- strongest / weakest theme 合理
- 可生成并回查 weekly review snapshot

### 11.6 推荐测试
- `WeeklyReviewAggregatorTest`
- `NextWeekSuggestionPolicyTest`
- `WeeklyReviewServiceTest`
- `WeeklyReviewControllerTest`

---

## 12. Phase 8：记忆回写模块

### 12.1 目标
支持 Python Agent 将结构化 reflection / summary 回写进 Java Core。

### 12.2 依赖
- `session_reflection`、`behavior_pattern_summary` 表可用
- session 与 user 模块已完成

### 12.3 产出内容

#### DTO
- `SaveSessionReflectionRequest`
- `SessionReflectionResponse`
- `SaveBehaviorPatternSummaryRequest`
- `BehaviorPatternSummaryResponse`

#### Service
- `MemoryWritebackService`

#### Controller
- `MemoryController`

#### Persistence
- `SessionReflectionDO`
- `BehaviorPatternSummaryDO`
- `SessionReflectionMapper`
- `BehaviorPatternSummaryMapper`

### 12.4 具体任务

#### Task 8-1：实现 session reflection 回写接口
接口：
- `POST /api/memory/session-reflections`

校验：
- session 必须存在
- userId 必须匹配
- confidence 范围合法

#### Task 8-2：实现 behavior summary 回写接口
接口：
- `POST /api/memory/behavior-patterns`

校验：
- user 必须存在
- summaryType 必填
- source 必填

#### Task 8-3：保证回写只入 memory 相关表
明确：
- 不允许直接改 `mastery_state`
- 不允许直接改 `daily_plan_snapshot`

### 12.5 验收标准
- Python 侧结构化结果可以安全入库
- 非法 session / user 会正确报错
- 核心事实表不会被 memory 回写污染

### 12.6 推荐测试
- `MemoryWritebackServiceTest`
- `MemoryControllerTest`

---

## 13. 推荐实现顺序

建议严格按下面顺序推进：

1. Phase 0：通用骨架
2. Phase 1：数据库 migration
3. Phase 2：用户画像
4. Phase 3：定级
5. Phase 4：学习内容
6. Phase 5：日计划与调度
7. Phase 6：会话与掌握状态
8. Phase 7：周复盘
9. Phase 8：记忆回写

原因：
- 后面的模块都依赖前面的数据结构和规则真相
- 尤其 session / mastery 是整个系统核心，不适合提前乱开工

---

## 14. 按文件类型看待开发顺序

如果按“文件类型”来理解，可以这么落：

### 第一层：通用底座
- enum
- response
- exception
- config

### 第二层：数据库与 DO/Mapper
- migration
- DO
- Mapper

### 第三层：领域规则
- scorer
- policy
- state machine
- calculator

### 第四层：应用服务
- service
- transaction
- orchestration

### 第五层：接口层
- request DTO
- response DTO
- controller

### 第六层：测试层
- migration test
- mapper test
- domain rule test
- service test
- controller test

---

## 15. 哪些任务最容易失控

### 15.1 日计划模块
这是最容易写散的地方。

因为它同时涉及：
- user_profile
- learning_item
- mastery_state
- priority formula
- rationale
- 持久化 snapshot

建议一定要先把领域规则写死，再写 service。

### 15.2 学习会话与掌握状态模块
这是最容易写崩的地方。

因为它同时涉及：
- attempt_log
- mastery_state
- score 更新
- 状态迁移
- 下次复习时间
- recommended_mode

建议先分成 4 个规则类，不要全塞进 `StudySessionService`。

### 15.3 周复盘模块
容易被写成“SQL 大杂烩”。

建议：
- Mapper 只查聚合数据
- 计算 strongest/weakest 和 suggestion 放到 domain/service 中

---

## 16. 当前里程碑定义

### Milestone 1：可启动底座
完成：
- Phase 0
- Phase 1

标志：
- 项目结构稳
- 数据库可初始化
- seed 数据可查

### Milestone 2：用户与定级闭环
完成：
- Phase 2
- Phase 3

标志：
- 用户可建档
- 定级结果可回写

### Milestone 3：计划生成闭环
完成：
- Phase 4
- Phase 5

标志：
- 可生成并查询日计划

### Milestone 4：学习执行闭环
完成：
- Phase 6

标志：
- session 可完成
- mastery_state 可更新

### Milestone 5：复盘与记忆回写闭环
完成：
- Phase 7
- Phase 8

标志：
- weekly review 可生成
- Python 结构化反思可回写

---

## 17. 每阶段完成后的检查问题

每做完一个 phase，都建议问自己：

1. 数据库表和接口契约是否一致？
2. DTO 字段是否和文档一致？
3. 规则是否有独立测试？
4. Service 是否承担了过多领域计算？
5. Mapper 是否写成了业务逻辑堆放点？
6. 返回结构是否统一？
7. 错误码是否统一？

---

## 18. 当前结论

这份任务拆解文档的价值，不在于“描述系统是什么”，而在于：

**把 `java-core` 从设计状态推进到可实施状态。**

也就是说，它解决的是：
- 先做什么
- 后做什么
- 哪部分最难
- 哪部分必须先稳定

这样你后面真正开写 Java 时，不会一上来就把 Controller、Service、Mapper、规则和 SQL 搅成一锅。

---

## 19. 下一步建议

如果你继续保持“先设计后实现”的节奏，那下一份最适合写的是：

**`java-core-package-design.md`**

如果你已经准备开始动手实现，那下一步就不是写新 spec，
而是从 `Phase 0 -> Phase 1` 开始，按这份 task breakdown 一步步做。
