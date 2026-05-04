# Tatoeba Sentence Enrichment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从 Tatoeba 开放语料库中提取英文例句及中文翻译，匹配到已有 `learning_item` 单词和短语，并生成 Flyway 迁移 SQL 更新 `examples` 字段。

**Architecture:** 用仓库根目录下的离线 Python 工具读取现有 seed migration 和本地 `tatoeba/` 数据，先生成可验证的 SQL 文件，再交给现有 Java Flyway 测试确认迁移可加载。脚本必须用小型 SQL 字符串解析器解析 seed 文件，不能用逐行正则，因为现有 seed SQL 含转义单引号、逗号和换行。

**Tech Stack:** Python 3.11 标准库、`unittest`、Flyway migration SQL、Spring Boot/H2 migration tests via Maven。

---

## 审查结论

原计划总体方向可行，但有几处会直接影响执行结果：

- `V21__enrich_examples_with_tatoeba.sql` 与当前已有 `V21__create_coach_memory_tables.sql` 冲突。生成脚本必须扫描现有 migration，默认从下一空闲版本开始；当前仓库应从 `V22` 开始。
- 原计划的逐行正则 `\('([wp]_\d+)',\s*'(\w+)',\s*'(.+?)',` 不可靠。现有数据包含 `'''d better`、`one''s`、逗号、中文解释换行等内容，逐行正则会漏项或错列。
- 目标数据不只是单词，也包含大量短语，例如 `be crazy about`、`make a speech`。只用 sentence token set 与 `word_set` 交集会漏掉短语，并可能把短语内容错误简化为单词匹配。
- Tatoeba `links.csv` 不能假设一定是英文 ID 指向中文 ID。构建链接时要同时处理 `eng -> cmn` 和 `cmn -> eng` 两种方向。
- `varchar(4096)` 在 PostgreSQL 里按字符限制，不是字节限制。不能用按字节截断加 `...` 的方式处理 JSON，这可能产生无效 JSON；应先限制句子长度，再按完整 example 条目裁剪。
- 计划缺少自动化测试。解析、匹配、SQL 转义和长度限制必须先用小 fixture 测试覆盖，再跑全量数据。

---

## Current Repository Facts

**Data files:**

| File | Path | Size | Format |
|------|------|------|--------|
| Sentences | `tatoeba/sentences.csv` | 748MB | `ID<TAB>Lang<TAB>Text` |
| Links | `tatoeba/links.csv` | 449MB | `SentenceID<TAB>TranslationID` |

**Existing migrations:**

- Seed vocabulary files are `java-core/src/main/resources/db/migration/V4__seed_vocabulary_part1.sql` through `V20__seed_vocabulary_part17.sql`.
- `V21__create_coach_memory_tables.sql` already exists.
- The generated enrichment migration should therefore default to `V22`, or a later version if new migrations have been added.

**Target schema:**

`learning_item.examples` is defined in `java-core/src/main/resources/db/migration/V2__expand_user_profile_and_create_core_tables.sql` as `varchar(4096) not null default '[]'`.

---

## File Structure

**Create:** `tools/enrich_examples.py`

Responsibility: parse existing seed SQL, load Tatoeba sentence/link data, match examples, generate and validate Flyway SQL.

**Create:** `tools/test_enrich_examples.py`

Responsibility: unit tests for SQL tuple parsing, Tatoeba link direction handling, single-word matching, phrase matching, SQL escaping, JSON length handling, and Flyway version selection.

**Modify:** `java-core/src/main/resources/db/migration/V22__enrich_examples_with_tatoeba_part1.sql` and later generated files

Responsibility: generated `UPDATE learning_item SET examples = ... WHERE item_code = ...;` statements.

Do not commit or modify the raw `tatoeba/*.csv` files. They are local source data and are currently untracked.

---

## Processing Design

### Seed SQL Parsing

Parse only files matching:

```text
java-core/src/main/resources/db/migration/V[0-9]*__seed_vocabulary*.sql
```

Required parser behavior:

- Read each file as UTF-8 text.
- Locate the `VALUES` section and parse tuple records character by character.
- Support SQL single-quoted strings with doubled quote escaping, for example `'''d better` -> `'d better` and `one''s` -> `one's`.
- Support commas and newlines inside string literals.
- Extract at least tuple fields `item_code`, `type`, `content`, `examples` by column order from `INSERT INTO learning_item (item_code, type, content, meaning_zh, difficulty, theme, tags, examples)`.
- Build `content_to_codes: dict[str, list[str]]`, keyed by normalized content. Do not assume content is globally unique.
- Keep both `WORD` and `PHRASE`; code prefix alone is not enough because existing seed data contains phrase-like content under `WORD`.

Normalization for content keys:

```python
def normalize_content(text: str) -> str:
    text = text.strip().lower()
    text = re.sub(r"\s+", " ", text)
    return text
```

### Tatoeba Indexing

Use streaming reads for the large CSV files.

Required behavior:

- Read `sentences.csv` as tab-delimited rows.
- Keep English sentences with `lang == "eng"` and Chinese sentences with `lang == "cmn"`.
- Apply sentence quality filters before storing English sentences:
  - `min_sentence_len <= len(text) <= max_sentence_len`
  - text contains only printable ASCII for English examples
  - text contains at least one alphabetic token
- Read `links.csv` and build `en_to_zh: dict[int, list[int]]` in both directions:
  - if left ID is English and right ID is Chinese, add left -> right
  - if right ID is English and left ID is Chinese, add right -> left
- Default behavior should only output examples with a Chinese translation. Add `--allow-missing-zh` only as an explicit opt-in.

### Matching Strategy

Use token-boundary matching, not substring matching.

Single-word items:

- Tokenize sentence with `r"[a-zA-Z]+(?:[-'][a-zA-Z]+)*"`.
- Match lowercased tokens exactly against normalized single-token contents.

Multi-word/phrase items:

- Normalize phrase punctuation conservatively for matching:
  - collapse whitespace
  - trim trailing dots in placeholders like `sb.`, `sth.` only for matching
  - skip phrases containing `...` because they are templates, not natural text targets
- Generate sentence n-grams up to the maximum token length of target phrases, capped at 8 tokens.
- Match exact normalized n-grams to target phrases.

Ranking examples:

1. Prefer examples with Chinese translation.
2. Prefer shorter English sentences.
3. Prefer sentences where the matched item appears once.
4. Deduplicate by English text per content key.
5. Keep at most `--max-examples` examples per content key, default 3.

### SQL Generation

Output files:

```text
java-core/src/main/resources/db/migration/V22__enrich_examples_with_tatoeba_part1.sql
java-core/src/main/resources/db/migration/V23__enrich_examples_with_tatoeba_part2.sql
...
```

Version selection:

- Scan `java-core/src/main/resources/db/migration/V*__*.sql`.
- Parse numeric Flyway versions.
- Default `--start-version` to `max(existing_versions) + 1`.
- If `--start-version` is supplied, fail fast if any generated version would collide with an existing file.

SQL format:

```sql
-- Enrich learning_item examples with Tatoeba sentence pairs.
-- Source: Tatoeba Project, https://tatoeba.org/, CC BY 2.0 FR.
-- Generated by tools/enrich_examples.py from local tatoeba/sentences.csv and tatoeba/links.csv.

UPDATE learning_item SET examples = '[{"en":"I have an appointment at 3 PM.","zh":"我下午3点有个预约。"}]' WHERE item_code = 'w_000001';
```

Generation rules:

- Use `json.dumps(examples, ensure_ascii=False, separators=(",", ":"))`.
- Replace CR/LF in sentences with single spaces before JSON serialization.
- Escape SQL single quotes in generated JSON and item codes by replacing `'` with `''`.
- Ensure `len(json_text) <= 4096` characters. If too long, drop the lowest-ranked full example first; do not truncate raw JSON bytes.
- Generate one `UPDATE` per `item_code`, including all item codes mapped from the matched normalized content.
- Generate no update for items without examples; leave their current `examples = '[]'` unchanged.
- Split files by `--updates-per-file`, default 5000.

---

## Implementation Tasks

### Task 1: Add Script Unit Tests

**Files:**

- Create: `tools/test_enrich_examples.py`

- [ ] **Step 1: Write failing parser tests**

Cover these seed SQL cases:

```python
def test_parse_seed_values_handles_escaped_quotes_commas_and_newlines():
    sql = """INSERT INTO learning_item (item_code, type, content, meaning_zh, difficulty, theme, tags, examples) VALUES
    ('w_000001', 'WORD', '''d better', '最好是；其后加不带；更好的', 1, 'textbook_primary', '["momo"]', '[]'),
    ('w_000002', 'WORD', 'contents of one''s home', 'meaning, with comma\nand newline', 5, 'ielts', '[]', '[]');
    """

    rows = list(parse_seed_insert_rows(sql))

    assert rows[0]["item_code"] == "w_000001"
    assert rows[0]["content"] == "'d better"
    assert rows[1]["content"] == "contents of one's home"
```

- [ ] **Step 2: Write failing matching tests**

Cover exact word matching, phrase matching, no substring false positive, and skipping template phrases with `...`.

- [ ] **Step 3: Write failing SQL generation tests**

Cover JSON serialization with Chinese text, SQL single quote escaping, `len(json_text) <= 4096`, full-example dropping, and Flyway version collision detection.

- [ ] **Step 4: Run tests and confirm they fail because `tools/enrich_examples.py` does not exist yet**

Run:

```powershell
python -m unittest discover -s tools -p "test_*.py" -v
```

Expected: FAIL with import errors or missing functions.

### Task 2: Implement Seed SQL Parsing

**Files:**

- Create: `tools/enrich_examples.py`
- Test: `tools/test_enrich_examples.py`

- [ ] **Step 1: Implement `parse_seed_insert_rows(sql_text: str) -> Iterator[dict[str, str]]`**

Use a character scanner with states for outside tuple, inside tuple, inside SQL string, and escaped doubled quote.

- [ ] **Step 2: Implement `load_learning_items(migration_dir: Path) -> dict[str, list[str]]`**

Read seed migration files, parse rows, normalize content, and map normalized content to all item codes.

- [ ] **Step 3: Run parser tests**

Run:

```powershell
python -m unittest tools.test_enrich_examples -v
```

Expected: parser tests PASS.

### Task 3: Implement Tatoeba Loading And Matching

**Files:**

- Modify: `tools/enrich_examples.py`
- Modify: `tools/test_enrich_examples.py`

- [ ] **Step 1: Implement tab-delimited sentence loading helpers**

Functions should accept file-like paths so tests can use temporary fixture files.

- [ ] **Step 2: Implement bidirectional link mapping**

Ensure fixtures prove both `eng -> cmn` and `cmn -> eng` rows produce the same `en_to_zh` mapping.

- [ ] **Step 3: Implement token and phrase matching**

Create indexes for single-token targets and phrase targets. Match n-grams up to 8 tokens.

- [ ] **Step 4: Implement ranking and deduplication**

Keep at most 3 examples by default and prefer translated, shorter examples.

- [ ] **Step 5: Run matching tests**

Run:

```powershell
python -m unittest tools.test_enrich_examples -v
```

Expected: parser and matching tests PASS.

### Task 4: Implement SQL Generation And Validation

**Files:**

- Modify: `tools/enrich_examples.py`
- Modify: `tools/test_enrich_examples.py`

- [ ] **Step 1: Implement `next_flyway_version(migration_dir: Path) -> int`**

Current expected value in this repo is `22` because `V21__create_coach_memory_tables.sql` exists.

- [ ] **Step 2: Implement `build_examples_json(examples: list[dict]) -> str`**

Use compact JSON, preserve Chinese characters, and drop full examples until the JSON string is at most 4096 characters.

- [ ] **Step 3: Implement `sql_quote(value: str) -> str` and `generate_update_sql(...)`**

Generate individual `UPDATE` statements for H2/PostgreSQL compatibility with existing Flyway tests.

- [ ] **Step 4: Implement `validate_generated_sql(path: Path)`**

Validation must parse generated update statements, unescape SQL strings, run `json.loads()`, check JSON shape, and check max length.

- [ ] **Step 5: Run all script tests**

Run:

```powershell
python -m unittest discover -s tools -p "test_*.py" -v
```

Expected: PASS.

### Task 5: Run Full Data Generation

**Files:**

- Modify generated files under: `java-core/src/main/resources/db/migration/`

- [ ] **Step 1: Run a dry-run summary first**

Run:

```powershell
python tools/enrich_examples.py --tatoeba-dir tatoeba --migration-dir java-core/src/main/resources/db/migration --dry-run
```

Expected output should include target item count, matched item count, examples count, first generated Flyway version, and skipped counts by reason.

- [ ] **Step 2: Inspect dry-run coverage**

Expected rough coverage is lower for phrases than for common single words. Do not treat 60-75% as guaranteed; report actual coverage.

- [ ] **Step 3: Generate migration files**

Run:

```powershell
python tools/enrich_examples.py --tatoeba-dir tatoeba --migration-dir java-core/src/main/resources/db/migration --updates-per-file 5000
```

Expected: generated files start at the next available Flyway version, currently `V22__enrich_examples_with_tatoeba_part1.sql`.

- [ ] **Step 4: Validate generated files with the script**

Run:

```powershell
python tools/enrich_examples.py --migration-dir java-core/src/main/resources/db/migration --validate-only
```

Expected: PASS with counts for files, update statements, and JSON examples.

### Task 6: Verify With Existing Java Migration Tests

**Files:**

- Generated SQL files under `java-core/src/main/resources/db/migration/`

- [ ] **Step 1: Run Flyway-focused tests**

Run:

```powershell
cd java-core
mvn test "-Dtest=FlywayMigrationTest,SchemaMigrationTest"
```

Expected: PASS. This confirms generated SQL can be loaded by the same H2 PostgreSQL-mode migration tests already used in the repo.

- [ ] **Step 2: If Flyway tests become too slow, measure before changing approach**

Do not skip migration tests. If the generated update volume makes tests impractical, first record runtime, then consider reducing generated statement count per file or adding a targeted migration smoke test strategy in a separate plan.

- [ ] **Step 3: Run script tests again after any fixes**

Run:

```powershell
python -m unittest discover -s tools -p "test_*.py" -v
```

Expected: PASS.

---

## Script CLI Contract

`tools/enrich_examples.py` should support:

```text
--tatoeba-dir tatoeba
--migration-dir java-core/src/main/resources/db/migration
--start-version <int>        # optional; default max existing Flyway version + 1
--updates-per-file 5000
--max-examples 3
--max-sentence-len 120
--min-sentence-len 12
--allow-missing-zh           # optional; default false
--dry-run
--validate-only
```

Dry-run output must include:

- number of parsed learning items
- number of unique normalized content keys
- number of English and Chinese sentences retained
- number of English sentences with Chinese translations
- number of item codes with examples
- generated Flyway version range
- skipped counts for no translation, quality filter, template phrase, JSON length, and duplicate sentence

---

## Validation Checklist

Before calling the implementation complete, verify all of the following:

- `python -m unittest discover -s tools -p "test_*.py" -v` passes.
- `python tools/enrich_examples.py --migration-dir java-core/src/main/resources/db/migration --validate-only` passes after generation.
- Generated migration versions do not collide with existing files; current first expected version is `V22`.
- Every generated `examples` value is valid JSON with objects shaped like `{"en": str, "zh": str}`.
- Every generated JSON string is at most 4096 characters.
- Generated SQL escapes single quotes correctly.
- At least 20 random generated rows have been manually sampled for English/Chinese alignment.
- `cd java-core; mvn test "-Dtest=FlywayMigrationTest,SchemaMigrationTest"` passes.

---

## Risks And Countermeasures

| Risk | Countermeasure |
|------|----------------|
| Flyway version collision | Auto-detect next version and fail fast on explicit collisions. Current repo already uses V21. |
| Bad seed SQL parsing | Use tested tuple parser instead of line regex. Cover escaped quote, comma, and newline fixtures. |
| Missing phrase examples | Build both single-token and n-gram phrase indexes. Skip template phrases that cannot naturally match. |
| False positive substring matches | Match tokens and normalized n-grams only, never raw substring search. |
| Wrong translation direction | Build `en_to_zh` from both link directions. |
| Invalid JSON after truncation | Drop full example objects until JSON fits; never byte-truncate serialized JSON. |
| Low coverage for rare phrases | Report actual coverage and skipped reasons; do not promise 60-75% as a hard acceptance threshold. |
| Generated migrations slow down tests | Measure `FlywayMigrationTest,SchemaMigrationTest` runtime before changing migration strategy. |
| Attribution gap | Include Tatoeba source/license comment in each generated SQL file. |
