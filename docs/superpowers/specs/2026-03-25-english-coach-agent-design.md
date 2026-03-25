# English Coach Agent 技术架构设计（MVP）

日期：2026-03-25
状态：draft
输入来源：`english-coach-design.md`

## 1. 文档目标

本文档将产品需求设计文档进一步落成一版适合个人练习和学习的技术架构方案。

目标不是做商业化完整产品，而是做一个能稳定跑通的、AI 驱动的英语教练 Agent 系统 MVP，用来练习：
- Agent 开发流程
- 规则引擎与 LLM 混合架构
- 可靠记忆设计
- 反思 / 纠错闭环
- 多模型接入适配
- Harness Engineering 与评测回放

---

## 2. 项目定位

该项目应定义为：

**一个 API-first 的英语教练 Agent 系统。**

系统以“定级 → 日计划 → 学习会话 → 状态更新 → 周复盘”的闭环为核心。

它不是单纯聊天应用，也不是完全交给大模型决策的黑盒 Agent，而是：

- **Java Core** 负责核心领域真相、状态机、规则调度、持久化
- **Python Agent Layer** 负责智能编排、模型调用、反思、纠错、实验能力
- **LLM** 负责增强体验与生成能力，不直接接管核心学习状态
- **Memory** 同时包含结构化学习状态记忆与会话摘要记忆
- **Harness** 用于评估系统行为、模型策略和流程可靠性

---

## 3. 技术目标

第一阶段重点不是功能多，而是把下面几件事做稳：

1. 跑通核心学习闭环
2. 明确规则层与 Agent 层边界
3. 实现可靠记忆，而不是把全部上下文塞进 prompt
4. 引入轻量反思与纠错循环
5. 支持多个大模型厂商的统一接入
6. 预留 Harness，用于回放和评测整个系统行为

---

## 4. 设计原则

### 4.1 规则控核，AI 增强
所有会影响核心学习状态的逻辑，优先交给可解释规则：
- 定级评分
- 日计划负载
- 复习优先级
- 掌握状态更新
- 周复盘节奏调整

AI 负责：
- 教练反馈自然化
- 错因解释润色
- 补充练习生成
- 会话摘要与反思
- 自由文本结构化抽取辅助

### 4.2 可靠记忆优先于长 prompt
优先建立可持久化、可校验、可查询的记忆层，而不是依赖一大段上下文硬塞给模型。

### 4.3 Agent 层不拥有事实主权
Python Agent 可以组织流程、调用模型、做反思与纠错，但不能直接绕过 Java Core 改写关键状态。

### 4.4 模型可替换
从第一天就做 provider adapter，不把系统绑死在单一厂商。

### 4.5 先做闭环，再做体验
先把 API 闭环跑通，再考虑 Web、语音、复杂页面与社交玩法。

---

## 5. 总体架构

建议采用 **四层架构 + 一个横切评测层**。

### 5.1 Java Core Domain Service
职责：
- 用户画像管理
- 内容项管理
- 轻量定级评分
- 日计划生成
- 记忆调度
- 掌握状态更新
- 学习记录写入
- 周复盘统计
- 核心 API 暴露

这是系统的“真相层”。

### 5.2 Python Agent Orchestrator
职责：
- 学习会话编排
- 调用 Java Core 获取计划与状态
- 调用 LLM 生成教练反馈、解释和补充练习
- 自由文本转结构化结果
- 触发 reflection / correction loop
- 管理 provider 路由与 fallback
- 提供可回放的 agent 执行入口

这是系统的“智能协调层”。

### 5.3 Memory Layer
分成两类：

#### 结构化记忆
由 Java 主导：
- UserProfile
- LearningItem
- MasteryState
- StudySession
- AttemptLog
- WeeklyReviewSnapshot

#### 摘要记忆
由 Python 生成、结构化落库：
- SessionReflection
- CoachingSummary
- BehaviorPatternSummary
- StrategyAdjustmentHint

### 5.4 Model / Tool Adapter Layer
统一抽象：
- ChatModelAdapter
- StructuredGenerationAdapter
- ExplanationAdapter
- ExerciseGenerationAdapter
- SummaryAdapter
- EmbeddingAdapter（第二阶段可选）

可对接：
- OpenAI
- Anthropic
- Gemini
- DeepSeek
- Qwen
- 本地模型（Ollama / vLLM）

### 5.5 Harness / Evaluation Layer
横切系统：
- 流程回放
- Prompt / policy 对比
- Provider 对比
- 故障注入
- 回归评测
- 成本 / 延迟 / 稳定性记录

---

## 6. 服务边界

### 6.1 Java Core 边界
Java 负责确定性逻辑和持久化，重点保持“可解释、可测试、可追踪”。

建议拆分为以下 Domain Services：
- `placement-service`
- `daily-plan-service`
- `memory-scheduler-service`
- `mastery-state-service`
- `feedback-analysis-service`
- `weekly-review-service`
- `content-service`
- `user-profile-service`

这些服务可以先做成一个 Spring Boot 单体中的模块化分层，而不是一开始拆成微服务。

### 6.2 Python Agent 边界
Python 负责 agentic workflow，不负责核心事实写入策略。

建议拆分为：
- `session_orchestrator`
- `coach_response_agent`
- `exercise_generation_agent`
- `reflection_agent`
- `correction_agent`
- `provider_router`
- `harness_runner`

注意：这里的“agent”是逻辑模块，不一定一开始就要搞成多 Agent 对话系统。

---

## 7. MVP 核心流程

### 7.1 首次建档与定级
流程：
1. 用户提交目标、可学习时间、偏好
2. Java Core 创建 `UserProfile`
3. Python 编排一个轻量定级会话
4. Java 根据结构化答题结果计算能力画像
5. Java 生成初始节奏建议
6. Python 用 LLM 将结果转成教练式反馈

### 7.2 每日计划生成
流程：
1. Java Core 读取用户画像、掌握状态、最近表现
2. 计算今日负载
3. 挑选复习项与新学项
4. 生成任务包与解释原因
5. Python 负责将任务包包装成自然会话入口

### 7.3 学习会话执行
流程：
1. Python 拉取日计划
2. 按顺序执行：热身复习 → 新学输入 → 即时检测 → 输出强化 → 收尾小测
3. 用户回答转结构化 attempt
4. Java 更新掌握状态
5. Python 根据结构化结果生成即时反馈
6. 会话结束后生成 session reflection

### 7.4 每周复盘
流程：
1. Java 聚合本周行为与表现数据
2. 输出结构化周报
3. Python 生成自然语言复盘总结与下周建议

---

## 8. 记忆设计

### 8.1 结构化记忆
建议保留以下核心实体：
- `user_profile`
- `learning_item`
- `mastery_state`
- `study_session`
- `attempt_log`
- `weekly_review_snapshot`

其中 `mastery_state` 是最核心的长期学习状态。

### 8.2 会话摘要记忆
建议新增：
- `session_reflection`
- `behavior_pattern_summary`

字段示例：
- `session_id`
- `user_id`
- `detected_patterns`
- `fatigue_signal`
- `recommended_adjustment`
- `coach_notes`
- `confidence`
- `created_at`

### 8.3 记忆使用原则
- 结构化记忆是主依据
- 摘要记忆是策略提示，不直接改写事实字段
- 摘要记忆应带置信度与来源
- Agent 层使用摘要记忆时，优先把它当作“提示”，而不是“真相”

---

## 9. 反思与纠错闭环

### 9.1 Reflection Loop
每次学习会话结束后执行轻量反思：
- 今日计划是否过重
- 哪类错误最集中
- 用户是“不会”还是“会但不稳”
- 哪类练习更有效
- 下次是否调整新学量或输出强度

输出应为结构化 reflection，而不是只生成一段散文。

### 9.2 Correction Loop
当模型输出与规则判断冲突时，触发纠错：

触发例子：
- LLM 说“基本正确”，但规则检测目标短语缺失
- 解释生成结果与题型不匹配
- 结构化抽取缺字段
- Provider 返回格式异常

纠错流程建议：
1. 标记当前结果 low-confidence
2. 执行二次结构化或二次解释
3. 若仍冲突，则回退到规则保守结论
4. 记录 correction trace，供后续 harness 分析

### 9.3 边界原则
- 反思与纠错可以影响下一轮策略建议
- 不能直接绕过规则层更新掌握状态主字段

---

## 10. 多模型接入设计

### 10.1 目标
让系统支持不同 AI 厂商模型，以便：
- 比较成本与效果
- 做 fallback
- 为不同任务选择合适模型
- 避免单厂商绑定

### 10.2 任务维度路由
建议按任务类型配置模型：
- `coach_feedback_model`
- `explanation_model`
- `exercise_generation_model`
- `reflection_model`
- `evaluation_model`

### 10.3 Provider Adapter 设计
统一接口：
- `generate_text()`
- `generate_structured()`
- `classify()`
- `summarize()`
- `health_check()`

Provider 侧处理：
- 鉴权
- 超时
- 重试
- 限流
- 结构化 schema 适配
- 错误封装

### 10.4 失败降级策略
- 主模型失败时切换备用模型
- 结构化生成失败时降级为更简单 schema
- LLM 全部不可用时，学习主流程仍可基于规则继续执行

---

## 11. Harness Engineering 设计

这一层是该项目非常值得练的部分。

### 11.1 Harness 目标
评估的不只是模型回答质量，而是**整个 Agent 系统行为质量**。

### 11.2 三类核心 Harness

#### A. 任务回放 Harness
固定：
- 用户画像
- 学习内容
- 答题轨迹
- 疲劳状态
- 错误模式

用于检查：
- 日计划是否合理
- 复习优先级是否稳定
- 会话编排是否符合预期

#### B. 策略评估 Harness
对比：
- 不同模型
- 不同 prompt
- 不同 reflection 策略
- 不同 correction 策略

指标：
- 解释质量
- 教练反馈自然度
- 练习是否超纲
- 状态调整是否偏激

#### C. 稳定性 Harness
模拟：
- Provider 超时
- 结构化输出损坏
- Java API 失败
- 会话中断
- 记忆写入失败

评估：
- 主流程可恢复性
- 降级效果
- 错误日志可追踪性

### 11.3 建议评估指标
- plan_relevance_score
- review_priority_consistency
- output_difficulty_match
- feedback_helpfulness
- correction_success_rate
- fallback_success_rate
- average_latency_ms
- token_cost_per_session

---

## 12. 技术栈建议

### 12.1 Java Core
建议：
- Java 21（若本机暂时是 17，可先用 17 起步，但建议尽快升到 21）
- Spring Boot 3
- Maven
- Spring Web
- Spring Validation
- MyBatis-Plus
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5

数据访问建议：
- 基础 CRUD 使用 MyBatis-Plus
- 复杂查询、统计与调度筛选使用自定义 Mapper SQL
- 领域规则不要塞进 Mapper 层

### 12.2 Python Agent
建议：
- Python 3.12
- FastAPI
- Pydantic
- httpx
- SQLAlchemy（如果 Python 侧也需要独立写库）
- pytest
- tenacity（重试）

Agent 编排框架可以先别重度依赖，第一版建议自己写轻量 orchestrator。需要时再接 LangGraph / PydanticAI / Haystack / DSPy 等。

### 12.3 基础设施
建议：
- Docker Compose
- PostgreSQL
- Redis（第二阶段可选，用于会话缓存 / 队列）
- OpenTelemetry（第二阶段）

---

## 13. 目录结构建议

### 13.1 Java Core
```text
/java-core
  /src/main/java/... 
    /api
    /application
    /domain
    /infrastructure
    /scheduler
    /config
  /src/test/java/...
```

### 13.2 Python Agent
```text
/python-agent
  /app
    /api
    /orchestrators
    /agents
    /adapters
    /schemas
    /memory
    /harness
    /tests
```

### 13.3 项目根目录
```text
/docs
  /superpowers/specs
/data
/seeds
/docker
```

---

## 14. MVP 范围确认

### 14.1 第一版必须实现
1. 用户建档 API
2. 轻量定级 API + 会话编排
3. 每日计划生成 API
4. 学习会话主流程 API
5. 掌握状态更新
6. 结构化学习记忆
7. 会话摘要记忆
8. 教练反馈 LLM 接口
9. 补充练习生成接口
10. 轻量 reflection / correction loop
11. 多模型 provider adapter
12. 基础 harness 回放能力

### 14.2 第一版暂不实现
- 正式前端
- 语音评分
- 实时语音对话
- 大规模题库平台化
- 向量长期记忆
- 多 Agent 群体协作
- 社交功能

---

## 15. 实现阶段建议

### Phase 1：Core Skeleton
- Java Core 工程初始化
- Python Agent 工程初始化
- PostgreSQL schema
- 用户建档 / 内容种子 / 掌握状态基础表

### Phase 2：闭环跑通
- 定级
- 日计划
- 学习会话
- AttemptLog
- MasteryState 更新
- 每周复盘

### Phase 3：AI 增强
- 教练反馈
- 错因解释
- 练习生成
- provider abstraction

### Phase 4：可靠性增强
- reflection loop
- correction loop
- fallback
- tracing
- harness 回放

### Phase 5：体验扩展
- Web UI
- 提醒适配器
- 发音 / 词典接入
- 更复杂内容生成策略

---

## 16. 关键风险

### 16.1 过度 Agent 化
不要一开始拆太多 agent 角色。先以单 orchestrator + 若干能力模块实现。

### 16.2 过度依赖 LLM
不要让模型直接决定核心状态。

### 16.3 记忆污染
摘要记忆不能直接当事实，要和结构化记忆分层。

### 16.4 架构过重
第一版保持模块化单体 + 一个 Python 服务即可，不要过早微服务化。

### 16.5 Harness 滞后
如果不从第一版就预留评测回放层，后面补会很痛苦。

---

## 17. 成功标准

MVP 成功标准：
- 能稳定完成“定级 → 日计划 → 学习 → 调整 → 复盘”闭环
- 核心学习状态由规则层可靠维护
- Agent 层能提供自然反馈与补充练习
- 系统有结构化记忆与会话摘要记忆
- 有基础反思 / 纠错闭环
- 可切换不同模型厂商
- 可对固定场景做回放和评测

---

## 18. 下一步

建议下一步进入实现计划阶段，输出：
1. 模块拆解与目录结构
2. 数据表设计
3. API 设计
4. Java Core 开发顺序
5. Python Agent 开发顺序
6. Harness 第一版案例集
7. 种子内容组织方式

