# Tatoeba 例句补充执行计划

> **For agentic workers:** 使用 superpowers:executing-plans 或 superpowers:subagent-driven-development 逐步执行本计划。

**目标：** 从 Tatoeba 开放语料库中提取英文例句（及中文翻译），匹配到已有的 84,243 个 learning_item 单词/短语上，生成 Flyway 迁移 SQL 更新 `examples` 字段。

**当前状态：** learning_item 表已有 84,243 条数据（V4~V20），但 `examples` 字段全部为空 `'[]'`。Tatoeba 数据已下载到 `tatoeba/` 目录。

---

## 数据源

| 文件 | 路径 | 大小 | 条数 | 格式 |
|------|------|------|------|------|
| 英文/中文句子 | `tatoeba/sentences.csv` | 748MB | 全语言约 1300 万条 | `ID\tLang\tText`（tab 分隔） |
| 翻译配对 | `tatoeba/links.csv` | 449MB | 2800 万条 | `SentenceID\tTranslationID`（tab 分隔） |

关键数据量：
- 英文句子（`eng`）：约 202 万条
- 中文句子（`cmn`）：约 8.5 万条

---

## 处理流程概览

```
[Step 1] 提取已有单词列表
    ↓
[Step 2] 构建句子索引（英文→中文翻译映射）
    ↓
[Step 3] 遍历英文句子，匹配单词，收集例句
    ↓
[Step 4] 生成 Flyway 迁移 SQL（UPDATE examples）
    ↓
[Step 5] 验证 SQL 正确性
```

---

## Step 1: 提取已有单词列表

从已生成的 Flyway SQL 文件中解析出所有 `item_code → content` 映射。

**输入：** `java-core/src/main/resources/db/migration/V[0-9]*__seed_vocabulary*.sql`（17 个文件）

**解析逻辑：**
- 逐行匹配正则：`\('([wp]_\d+)',\s*'(\w+)',\s*'(.+?)',`
- 提取 `item_code`、`type`、`content`
- 构建两个结构：
  - `word_to_codes: dict[str, list[str]]` — 单词（小写）→ item_code 列表（去重合并后同一单词可能对应多个 item_code？不会，因为已去重，一个单词只有一个 item_code）
  - `word_set: set[str]` — 所有单词的小写形式，用于快速查重

**输出：** 内存中的 `word_to_codes` 字典，约 84,243 条

---

## Step 2: 构建句子索引

从 `tatoeba/sentences.csv` 和 `tatoeba/links.csv` 构建英文句子到中文翻译的映射。

**处理 sentences.csv：**
- 逐行解析：`ID \t Lang \t Text`
- 筛选 `lang == "eng"` 的句子，存入 `en_sentences: dict[int, str]`
- 筛选 `lang == "cmn"` 的句子，存入 `zh_sentences: dict[int, str]`

**处理 links.csv：**
- 逐行解析：`SourceID \t TargetID`
- 构建 `en_to_zh: dict[int, list[int]]` — 英文句子 ID → 中文翻译句子 ID 列表
- 只保留 SourceID 在 `en_sentences` 中且 TargetID 在 `zh_sentences` 中的配对

**内存优化注意事项：**
- `en_sentences` 约 202 万条，每条约 100 字节，约 200MB
- `zh_sentences` 约 8.5 万条，很小
- `en_to_zh` 约 2800 万条配对，但只需保留有中文翻译的英文句子
- 如果内存不足，可以分批处理：先扫描 links 构建有效英文句子 ID 集合，再只加载这些句子

**输出：** `en_sentences`、`zh_sentences`、`en_to_zh` 三个字典

---

## Step 3: 匹配单词并收集例句

遍历所有英文句子，提取句子中的单词，检查是否在目标词汇表中。

**单词提取逻辑：**
```python
import re

def extract_words(sentence: str) -> set[str]:
    """从句子中提取单词（小写），忽略标点和数字"""
    tokens = re.findall(r"[a-zA-Z]+(?:[-'][a-zA-Z]+)*", sentence)
    return {t.lower() for t in tokens if len(t) >= 2}
```

**匹配逻辑：**
```python
# 每个单词最多保留 3 个例句
MAX_EXAMPLES = 3
word_examples: dict[str, list[dict]] = defaultdict(list)

for sent_id, en_text in en_sentences.items():
    words_in_sentence = extract_words(en_text)
    matched_words = words_in_sentence & word_set

    for word in matched_words:
        if len(word_examples[word]) >= MAX_EXAMPLES:
            continue

        # 查找中文翻译
        zh_text = ""
        if sent_id in en_to_zh:
            for zh_id in en_to_zh[sent_id]:
                if zh_id in zh_sentences:
                    zh_text = zh_sentences[zh_id]
                    break  # 取第一个翻译

        word_examples[word].append({
            "en": en_text,
            "zh": zh_text
        })
```

**性能预估：**
- 遍历 202 万英文句子
- 每句提取单词 + set 交集操作
- 预计耗时 3-5 分钟（纯内存操作）

**筛选策略（可选优化）：**
- 优先保留短句（< 80 字符），更适合学习
- 优先保留有中文翻译的句子
- 跳过包含非 ASCII 字符的英文句子（可能是错误数据）

**输出：** `word_examples` 字典，key 为小写单词，value 为最多 3 个例句

---

## Step 4: 生成 Flyway 迁移 SQL

将收集到的例句生成 `UPDATE` SQL 语句。

**输出文件：** `java-core/src/main/resources/db/migration/V21__enrich_examples_with_tatoeba.sql`

**SQL 格式：**
```sql
-- Enrich learning_item examples with Tatoeba sentence pairs
-- Generated from Tatoeba open corpus (CC BY 2.0 FR)

UPDATE learning_item SET examples = '[{"en":"I have an appointment at 3 PM.","zh":"我下午3点有个预约。"}]' WHERE item_code = 'w_000001';
UPDATE learning_item SET examples = '[{"en":"She is very efficient at her job.","zh":"她工作效率很高。"},{"en":"We need an efficient system.","zh":"我们需要一个高效的系统。"}]' WHERE item_code = 'w_000002';
```

**注意事项：**
- `examples` 字段类型为 `varchar(4096)`，单条不能超过 4096 字节
- SQL 单引号需要转义（`'` → `''`）
- JSON 中的双引号不需要转义（在 SQL 单引号字符串内）
- 句子中的换行符替换为空格
- 只为有匹配例句的单词生成 UPDATE（预计覆盖 60-80% 的单词）
- 没有匹配到例句的单词保持 `examples = '[]'` 不变

**批量处理：**
- 每 5000 条 UPDATE 拆分为一个文件
- 如果数据量大，拆分为 V21、V22... 多个文件

---

## Step 5: 验证

执行以下验证检查：

1. **语法检查：** 解析生成的 SQL 文件，确保每条 UPDATE 语句语法正确
2. **JSON 格式检查：** 提取 examples 值，用 `json.loads()` 验证是合法 JSON
3. **长度检查：** 确保每条 examples 不超过 4096 字节
4. **覆盖率统计：** 统计有多少单词获得了例句
5. **抽样检查：** 随机抽取 20 条，人工确认英文句子和中文翻译匹配正确

---

## 执行脚本结构

创建 `tools/enrich_examples.py`，主函数结构：

```python
def main():
    # Step 1: 从 SQL 文件解析单词列表
    word_to_codes = parse_existing_sql_files()

    # Step 2: 构建句子索引
    en_sentences, zh_sentences, en_to_zh = build_sentence_index()

    # Step 3: 匹配并收集例句
    word_examples = collect_examples(word_to_codes, en_sentences, zh_sentences, en_to_zh)

    # Step 4: 生成 SQL
    generate_update_sql(word_to_codes, word_examples)

    # Step 5: 输出统计
    print_summary(word_to_codes, word_examples)
```

**脚本参数：**
```
--max-examples 3          # 每个单词最多保留几个例句
--max-sentence-len 120    # 例句最大字符数
--tatoeba-dir tatoeba     # Tatoeba 数据目录
--sql-dir java-core/src/main/resources/db/migration  # SQL 输出目录
--start-version 21        # 起始 Flyway 版本号
```

---

## 预期结果

| 指标 | 预估值 |
|------|--------|
| 有例句的单词覆盖率 | 60-75%（约 5-6 万个单词） |
| 总例句条数 | 约 15-20 万条（每词平均 3 条） |
| 生成 SQL 文件数 | 约 10-15 个（每文件 5000 条 UPDATE） |
| 脚本运行时间 | 约 5-8 分钟 |

---

## 风险与对策

| 风险 | 对策 |
|------|------|
| 内存不足（202万句子+2800万配对） | 分批加载：先扫描 links 构建有效 ID 集，再只加载需要的句子 |
| examples 超过 4096 字节限制 | 截断到 4090 字节 + `...` |
| 句子质量参差不齐 | 优先保留短句（<80字符），跳过含非 ASCII 的英文句子 |
| 同一单词在不同 item_code 出现 | 不会，因为已去重，每个单词只有一个 item_code |
| Flyway 版本号冲突 | 确认 V21 未被占用后使用，或用更高版本号 |
