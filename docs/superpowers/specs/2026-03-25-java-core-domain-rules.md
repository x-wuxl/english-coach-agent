# Java Core 领域规则设计

日期：2026-03-25  
状态：draft  
适用范围：`english-coach-agent/java-core`

---

## 1. 文档目标

这份文档用于定义 `java-core` 的核心领域规则。

如果说：
- 数据库文档定义了“存什么”
- API 契约文档定义了“怎么进出”

那这份文档定义的就是：

**系统到底如何判断、如何计算、如何迁移状态。**

这份文档重点覆盖：
- 定级评分规则
- 日负载计算规则
- 复习优先级规则
- 掌握状态迁移规则
- 周复盘聚合规则
- 错误类型归一化规则
- 反思回写校验规则

目标是让后续 Java 实现时：
- 规则可解释
- 可测试
- 可追踪
- 可迭代

---

## 2. 规则设计总原则

### 2.1 规则优先于模型主观判断
凡是影响核心学习状态的逻辑，优先由规则决定。

包括：
- 等级画像
- 日计划负载
- 复习项优先级
- 掌握状态更新
- 周复盘节奏建议

### 2.2 规则必须可解释
每个核心决策最好都能回答：
- 为什么今天安排这些内容
- 为什么这个学习项现在优先复习
- 为什么它从 `LEARNING` 变成了 `WEAK_OUTPUT`
- 为什么下周建议减量或加量

### 2.3 第一版宁可简单，也不要玄学
第一版的目标是稳定，不是“看起来特别聪明”。

所以规则设计优先：
- 少变量
- 好调试
- 可打印日志
- 可人工复核

### 2.4 规则与摘要记忆分层
- 结构化事实由规则消费
- Python 回写的 reflection / summary 只作为策略提示
- 摘要不能直接覆盖核心事实字段

---

## 3. 定级评分规则

## 3.1 输入
输入来源：`PlacementAssessmentRequest`

每道题的结构化字段至少包括：
- `section`
- `result`
- `responseTimeMs`
- `hintUsed`
- `responseText`（输出题可选）

定级分为四个维度：
- `vocab`
- `grammar`
- `reading`
- `output`

---

## 3.2 输出
定级结果包括：
- `overallLevel`
- `vocabLevel`
- `grammarLevel`
- `readingLevel`
- `outputLevel`
- `weaknesses`
- `suggestedDailyRhythm`

---

## 3.3 第一版评分思路
第一版不做复杂自适应定级。

采用：
- 分 section 统计正确率
- 输出题额外参考 hint 和 responseText
- 加上反应时间作为轻微修正项
- 最终映射到等级区间

---

## 3.4 维度分数计算
### 3.4.1 基础分
每个 section 先计算基础正确率：

```text
section_accuracy = correct_count / total_count
```

结果区间：
- 0.00 ~ 1.00

### 3.4.2 提示惩罚
如果某 section 中提示使用较多，则轻微降分：

```text
hint_penalty = hint_used_count / total_count * 0.10
```

### 3.4.3 反应时间修正
如果平均反应时间明显过慢，则轻微降分：

```text
slow_penalty = min(avg_response_time_ms / benchmark_ms - 1, 1) * 0.05
```

其中 benchmark 可以按 section 先写死：
- vocab: 4000 ms
- grammar: 5000 ms
- reading: 8000 ms
- output: 12000 ms

### 3.4.4 section 最终分
```text
section_score = section_accuracy - hint_penalty - slow_penalty
```

最终 clamp 到 `[0, 1]`。

---

## 3.5 等级映射建议
第一版建议映射区间如下：

- `0.00 ~ 0.19` -> A1-
- `0.20 ~ 0.34` -> A1
- `0.35 ~ 0.49` -> A1+
- `0.50 ~ 0.64` -> A2
- `0.65 ~ 0.74` -> A2+
- `0.75 ~ 0.84` -> B1
- `0.85 ~ 0.92` -> B1+
- `0.93 ~ 1.00` -> B2

第一版不用往 B2 以上走。

---

## 3.6 overallLevel 计算
第一版建议加权：

```text
overall_score =
0.30 * vocab_score +
0.25 * grammar_score +
0.20 * reading_score +
0.25 * output_score
```

说明：
- output 权重不能太低，因为这个项目强调“会认不会用”的问题
- reading 第一版比重可以略低

最终将 `overall_score` 映射为 `overallLevel`。

---

## 3.7 weaknesses 提取规则
按以下逻辑提取：
- 若 `output_score < 0.50` -> `OUTPUT_WEAKNESS`
- 若 `grammar_score < 0.50` -> `GRAMMAR_UNSTABLE`
- 若 `vocab_score < 0.50` -> `VOCAB_WEAKNESS`
- 若 output 明显低于 recognition（例如 vocab 高、output 低） -> `KNOW_BUT_CANNOT_USE`

`weaknesses` 最多返回 3 个。

---

## 3.8 suggestedDailyRhythm 规则
根据 `overallLevel` 和 `outputLevel` 给出建议：

### 低水平（A1 / A1+）
- newItems: 4~6
- reviewItems: 8~12
- outputTasks: 1~2

### 中低水平（A2 / A2+）
- newItems: 6~8
- reviewItems: 10~15
- outputTasks: 2~3

### B1 及以上
- newItems: 6~10
- reviewItems: 10~16
- outputTasks: 3~4

若 `outputLevel` 明显低于 `overallLevel`，则：
- 新学量不变或略减
- outputTasks +1

---

## 4. 日负载计算规则

## 4.1 输入
- `user_profile.daily_minutes`
- 最近 3 天完成率
- 最近 3 天正确率
- 最近 3 天 fatigue signal
- 连续学习天数
- 是否存在大量 overdue review

---

## 4.2 输出
输出三类数量：
- `new_items_count`
- `review_items_count`
- `output_task_count`

并输出：
- `load_decision`
- `load_reason`

---

## 4.3 基础负载档位
按 `daily_minutes` 先确定基础档位：

### 5~10 分钟
- new: 2~4
- review: 5~8
- output: 0~1

### 11~20 分钟
- new: 4~6
- review: 8~12
- output: 1~2

### 21~30 分钟
- new: 6~8
- review: 10~15
- output: 2~3

### 31~45 分钟
- new: 8~10
- review: 12~18
- output: 3~4

第一版最多只支持到 45 分钟档位，超过也先按这一档算。

---

## 4.4 动态修正规则

### 4.4.1 最近完成率高
若最近 3 天完成率 `>= 0.85`：
- `new_items_count +1`
- `review_items_count +1`

### 4.4.2 最近正确率高
若最近 3 天正确率 `>= 0.85`：
- `new_items_count +1`

### 4.4.3 最近疲劳明显
若最近 3 天 fatigue 中 `medium/high` 占比高：
- `new_items_count -2`
- `output_task_count -1`
- `review_items_count` 不减或略增

### 4.4.4 连续中断或完成率低
若最近 3 天完成率 `< 0.50`：
- `new_items_count -2`
- `review_items_count -1 ~ +2`（取决于 due 量）
- `output_task_count -1`

### 4.4.5 overdue review 很多
若到期未复习项数量明显超阈值：
- `review_items_count +3`
- `new_items_count -1`

---

## 4.5 负载模式输出
第一版统一输出三种：
- `LIGHT`
- `NORMAL`
- `BOOST`

判定建议：
- 修正后显著低于基础值 -> `LIGHT`
- 接近基础值 -> `NORMAL`
- 高于基础值 -> `BOOST`

---

## 5. 复习优先级规则

## 5.1 输入
对单个 `mastery_state` 计算优先级时，输入主要包括：
- `next_review_at`
- `forget_risk`
- 最近错误情况
- output gap
- 当前主题权重
- 最近曝光惩罚

---

## 5.2 due_score 计算
建议：

### 未到期
- 未到期很多：0.0 ~ 0.2

### 刚到期
- 到期但未超时很多：0.5 ~ 0.7

### 明显超时
- 超过较久：0.8 ~ 1.0

可用一个简单函数：

```text
if next_review_at is null -> 0.6
if now < next_review_at -> 0.2
if now >= next_review_at and overdue_hours < 24 -> 0.7
if overdue_hours >= 24 -> 1.0
```

---

## 5.3 recent_error_score
统计最近 N 次 attempt 中的错误占比。

```text
recent_error_score = recent_wrong_count / recent_attempt_count
```

若最近尝试太少，则回退为 0。

---

## 5.4 output_gap_score
用于识别“会认不会用”。

```text
output_gap_score = max(recognition_score - output_score, 0)
```

再归一到 `[0,1]`。

---

## 5.5 recent_exposure_penalty
如果这个 item 刚刚练过，不要马上重复推太高。

建议：
- 1 小时内练过：0.8
- 6 小时内练过：0.4
- 24 小时内练过：0.2
- 超过 24 小时：0.0

---

## 5.6 最终优先级公式
第一版延续需求文档中的解释型公式：

```text
review_priority =
0.30 * due_score +
0.25 * forget_risk +
0.20 * recent_error_score +
0.15 * output_gap_score +
0.10 * theme_weight -
0.10 * recent_exposure_penalty
```

最终结果 clamp 到合理区间即可。

---

## 5.7 排序规则
按 `review_priority desc` 排序。

若分数相同，则：
1. `next_review_at` 更早的优先
2. `status = WEAK_OUTPUT` 的优先
3. 最近错误更多的优先

---

## 6. 掌握状态迁移规则

## 6.1 目标状态
- `NEW`
- `LEARNING`
- `REVIEWING`
- `WEAK_OUTPUT`
- `MASTERED`
- `ARCHIVED`

---

## 6.2 基础思路
掌握状态由这些因素共同决定：
- 正确率
- 连续正确次数
- recognition / recall / output 三种分数
- 是否存在长期稳定表现
- 是否存在“识别高但输出低”

---

## 6.3 初始规则
- 学习项第一次被纳入计划或第一次产生 attempt -> `NEW`
- 一旦有第一次真实接触后 -> `LEARNING`

---

## 6.4 从 `LEARNING` 到 `REVIEWING`
当满足下面条件时，可进入 `REVIEWING`：
- 最近若干次识别题正确率较高
- `recognition_score >= 0.70`
- `correct_streak >= 2`
- `output_score` 不明显拖后腿

---

## 6.5 进入 `WEAK_OUTPUT`
这是第一版必须重点识别的状态。

满足以下条件之一可进入：
- `recognition_score >= 0.70` 且 `output_score < 0.45`
- 最近 3 次输出相关任务连续错误
- 识别题稳定正确，但中译英 / 造句持续差

一旦进入 `WEAK_OUTPUT`：
- 复习优先级应提高
- `recommended_mode` 应偏向输出型练习

---

## 6.6 从 `REVIEWING` 到 `MASTERED`
满足以下条件时可升级：
- `recognition_score >= 0.85`
- `output_score >= 0.70`
- `correct_streak >= 4`
- 最近一段时间表现稳定
- `forget_risk` 较低

---

## 6.7 从 `MASTERED` 回退
如果 `MASTERED` 项连续出错，则应回退：
- 若只是偶发错 1 次：保持 `MASTERED`，但提高 `forget_risk`
- 若近期连续错 2 次以上：回退到 `REVIEWING`
- 若表现为明显输出掉线：回退到 `WEAK_OUTPUT`

---

## 6.8 进入 `ARCHIVED`
第一版可以先保守处理。

建议条件：
- 长期稳定正确
- 最近一段时间无明显遗忘
- 输出能力也稳定

但 MVP 阶段可以先**不主动迁到 `ARCHIVED`**，只保留枚举和值域，避免过早复杂化。

---

## 7. 分数更新规则

## 7.1 recognition_score
适用于：
- 释义识别
- 选择题
- 匹配题
- 基础填空（偏识别）

建议：
- 正确则上升
- 错误则下降
- 带 hint 正确只小幅上升

---

## 7.2 output_score
适用于：
- 中译英
- 造句
- 情景补全
- 开放输出题

建议：
- 正确上升幅度大于 recognition_score
- 错误下降也更明显
- 这项用于判断“会认不会用”

---

## 7.3 forget_risk
建议基于以下因素综合调整：
- 最近正确率低 -> 上升
- 长时间未接触 -> 上升
- 最近连续正确 -> 下降
- 输出弱 -> 略上升

第一版不要求复杂遗忘曲线，保证方向正确即可。

---

## 8. 下次复习时间规则

## 8.1 基础间隔
第一版可用以下基础间隔：
- 学后当天 / 次日
- 第 1 天
- 第 3 天
- 第 7 天
- 第 14 天
- 第 30 天

---

## 8.2 表现修正
### 表现优秀
若：
- 正确
- 反应快
- 输出也正确

则：
- 间隔延后

### 表现一般
若：
- 识别能对
- 输出不稳

则：
- 间隔保持不变或轻微缩短

### 表现差
若：
- 答错
- 提示后才会
- 反应明显偏慢

则：
- 间隔提前
- 推荐练习模式切换为更针对性的形式

---

## 8.3 recommended_mode 规则
### 新学 / 不稳定
- `meaning_intro`
- `matching`
- `recognition_quiz`

### 识别稳定
- `fill_blank`
- `collocation_check`

### 输出弱
- `cn_to_en`
- `sentence_building`
- `scenario_response`

### 反复错误
- `error_contrast`
- `micro_scenario_retry`

---

## 9. 周复盘聚合规则

## 9.1 输入
聚合范围：一周内

主要来源：
- `study_session`
- `attempt_log`
- `mastery_state`
- `learning_item`

---

## 9.2 输出
- `completion_rate`
- `study_minutes`
- `new_items_count`
- `review_items_count`
- `high_frequency_error_types`
- `strongest_themes`
- `weakest_themes`
- `next_week_suggestion`

---

## 9.3 completion_rate 计算
建议：

```text
completion_rate = completed_sessions / planned_sessions
```

如果第一版没有严格的 planned_sessions 管理，也可以先退化为：

```text
completion_rate = completed_plan_count / generated_plan_count
```

---

## 9.4 strongest_themes / weakest_themes
基于 theme 维度聚合：
- 正确率
- 输出表现
- 错误率

建议简单打分：

```text
theme_score = 0.5 * accuracy + 0.3 * output_performance - 0.2 * error_rate
```

分高的作为 strongest，分低的作为 weakest。

---

## 9.5 high_frequency_error_types
统计一周 attempt 中错误类型频次，取 top N。

第一版建议 `N = 3`。

---

## 9.6 next_week_suggestion 规则
根据：
- 完成率
- 正确率
- 疲劳反馈
- output weakness

输出建议：
- `load`: `reduce / keep / increase`
- `outputRatio`: `reduce / keep / increase`
- `reviewFocus`: list

### 简单规则
- 完成率高 + 正确率高 + 疲劳低 -> `increase`
- 完成率中等 + 正确率中等 -> `keep`
- 完成率低或疲劳高 -> `reduce`
- 若输出弱明显 -> `outputRatio = increase`

---

## 10. 错误类型归一化规则

## 10.1 目标
不要让错误类型在日志里无限发散。

第一版统一映射到有限集合：
- `VOCAB_CONFUSION`
- `COLLOCATION_ERROR`
- `TENSE_ERROR`
- `ARTICLE_ERROR`
- `WORD_ORDER_ERROR`
- `OUTPUT_WEAKNESS`
- `UNKNOWN`

---

## 10.2 归一化规则
如果输入错误类型来自：
- 规则判断
- Python 结构化返回
- 前端/调用方传值

统一做 normalize。

例如：
- `collocation`
- `collocation_error`
- `搭配错误`

都归一到：
- `COLLOCATION_ERROR`

---

## 11. 反思回写校验规则

## 11.1 原则
Python 回写的 reflection / summary 可以入库，但不能直接改核心事实。

### 允许写入
- `session_reflection`
- `behavior_pattern_summary`

### 不允许直接覆盖
- `mastery_state.status`
- `mastery_state.recognition_score`
- `mastery_state.output_score`
- `mastery_state.next_review_at`

---

## 11.2 session reflection 校验
至少校验：
- `sessionId` 存在
- `userId` 与 session 一致
- `confidence` 在 0~1 范围内
- `source` 非空

### detected_patterns
第一版建议允许自由字符串数组，但后续可逐步收敛成有限集合。

---

## 11.3 behavior summary 校验
至少校验：
- `userId` 存在
- `summaryType` 非空
- `source` 非空
- `confidence` 在 0~1

---

## 12. 第一版必须覆盖的测试场景

## 12.1 定级规则测试
- 高正确率应映射较高等级
- output 明显偏低应拉低 outputLevel
- 提示多 / 反应慢应轻微降分

## 12.2 日负载测试
- 完成率高时应加量
- 疲劳高时应减新学量
- overdue 多时应提高复习量

## 12.3 复习优先级测试
- overdue + weak_output 的项优先级应高于 fresh stable item
- recent exposure 应带来降权

## 12.4 掌握状态迁移测试
- 识别稳定但输出弱 -> 进入 `WEAK_OUTPUT`
- 多次稳定正确 -> 进入 `REVIEWING` / `MASTERED`
- mastered 连续出错 -> 回退

## 12.5 周复盘测试
- 错误类型统计正确
- strongest / weakest theme 合理
- 节奏建议符合完成率与疲劳情况

---

## 13. 当前结论

`java-core` 第一版最关键的规则，不是“尽量聪明”，而是：

- 可解释
- 可验证
- 可落库
- 可调试
- 可和 Python Agent 清晰分层

所以这一版规则文档的目标就是：
**把核心判断逻辑写死、写清、写稳。**

---

## 14. 下一步建议

在这份领域规则文档确认后，下一份最适合写的是：

**`java-core-package-design.md`** 或 **`java-core-task-breakdown.md`**

如果你想继续偏设计，就先写 package design。  
如果你想开始准备进入实现，就先写 task breakdown。
