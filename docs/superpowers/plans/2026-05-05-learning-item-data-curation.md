# Learning Item Data Curation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clean the Flyway seed vocabulary by deleting low-value learning items, enriching empty Chinese meanings, and correcting item types for phrase-like entries.

**Architecture:** Keep source SQL as the final artifact, but do all data decisions through repeatable scripts and intermediate JSONL audit files. Parse `INSERT INTO learning_item` rows into structured records, generate candidate review files, call an LLM in small validated batches, then write accepted changes back to `java-core/src/main/resources/db/migration/V5__seed_vocabulary_part1.sql` through `V21__seed_vocabulary_part17.sql`.

**Tech Stack:** Python 3 standard library for SQL parsing/rewriting and JSONL validation, Maven/Flyway/H2 for migration verification, existing Java tests via `mvn clean package`.

---

## Batch Size Policy

Use 20 items per LLM request by default. Increase to 30 only for entries with no examples and short content. Do not use 50-100 for this dataset because many rows include long `examples` JSON; large batches increase the risk of truncated JSON, inconsistent decisions, and high context pressure.

Each LLM prompt must contain only the current batch. Previous batches are persisted to JSONL on disk, so they do not need to stay in model context. The scripts merge results by `item_code`, making the process resumable.

## File Structure

- Create: `tools/learning_item_curation/extract_learning_items.py`
  - Parses Flyway seed SQL into structured JSONL records.
- Create: `tools/learning_item_curation/generate_candidates.py`
  - Produces deletion candidates, type-change candidates, and meaning-enrichment batches.
- Create: `tools/learning_item_curation/validate_llm_results.py`
  - Validates JSONL output from the LLM before applying it.
- Create: `tools/learning_item_curation/apply_curation.py`
  - Rewrites migration SQL after validation.
- Create: `tools/learning_item_curation/prompts/meaning_enrichment_prompt.md`
  - Stores the production prompt used for each LLM batch.
- Create: `data/learning_item_curation/`
  - Stores generated JSONL audit files and LLM outputs. Commit only curated review artifacts if useful; otherwise keep generated files untracked if they become too large.
- Modify: `java-core/src/main/resources/db/migration/V5__seed_vocabulary_part1.sql` through `java-core/src/main/resources/db/migration/V21__seed_vocabulary_part17.sql`
  - Final SQL data changes.

---

### Task 1: Build SQL Extraction Tool

**Files:**
- Create: `tools/learning_item_curation/extract_learning_items.py`
- Output: `data/learning_item_curation/learning_items.jsonl`

- [ ] **Step 1: Create the extraction script**

Use `apply_patch` to create `tools/learning_item_curation/extract_learning_items.py`:

```python
#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MIGRATION_DIR = ROOT / "java-core/src/main/resources/db/migration"
OUTPUT = ROOT / "data/learning_item_curation/learning_items.jsonl"


def split_sql_values(row: str) -> list[str]:
    fields: list[str] = []
    current: list[str] = []
    in_string = False
    i = 0
    while i < len(row):
        ch = row[i]
        if in_string:
            current.append(ch)
            if ch == "'":
                if i + 1 < len(row) and row[i + 1] == "'":
                    current.append(row[i + 1])
                    i += 2
                    continue
                in_string = False
            i += 1
            continue
        if ch == "'":
            in_string = True
            current.append(ch)
            i += 1
            continue
        if ch == ",":
            fields.append("".join(current).strip())
            current = []
            i += 1
            if i < len(row) and row[i] == " ":
                i += 1
            continue
        current.append(ch)
        i += 1
    fields.append("".join(current).strip())
    return fields


def unquote(value: str) -> str:
    if not value.startswith("'") or not value.endswith("'"):
        raise ValueError(f"Expected SQL string literal, got {value[:80]}")
    return value[1:-1].replace("''", "'")


def migration_version(path: Path) -> int:
    match = re.match(r"V(\d+)__", path.name)
    if not match:
        raise ValueError(f"Unexpected migration filename: {path.name}")
    return int(match.group(1))


def iter_learning_items() -> list[dict[str, object]]:
    records: list[dict[str, object]] = []
    files = sorted(MIGRATION_DIR.glob("V*.sql"), key=migration_version)
    for path in files:
        if migration_version(path) < 5:
            continue
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.startswith("('"):
                continue
            terminator = line[-2:]
            if terminator not in {"),", ");"}:
                raise ValueError(f"Unexpected row terminator at {path}:{line_number}")
            fields = split_sql_values(line[1:-2])
            if len(fields) != 8:
                raise ValueError(f"Expected 8 fields at {path}:{line_number}, got {len(fields)}")
            examples_raw = unquote(fields[7])
            try:
                examples = json.loads(examples_raw)
            except json.JSONDecodeError as exc:
                raise ValueError(f"Invalid examples JSON at {path}:{line_number}: {exc}") from exc
            records.append(
                {
                    "file": path.name,
                    "line": line_number,
                    "item_code": unquote(fields[0]),
                    "type": unquote(fields[1]),
                    "content": unquote(fields[2]),
                    "meaning_zh": unquote(fields[3]),
                    "difficulty": int(fields[4]),
                    "theme": unquote(fields[5]),
                    "tags": json.loads(unquote(fields[6])),
                    "examples": examples,
                }
            )
    return records


def main() -> None:
    records = iter_learning_items()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8") as out:
        for record in records:
            out.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n")
    print(f"wrote {len(records)} records to {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Run extraction**

Run:

```bash
python3 tools/learning_item_curation/extract_learning_items.py
```

Expected output:

```text
wrote 84243 records to data/learning_item_curation/learning_items.jsonl
```

- [ ] **Step 3: Sanity check extracted data**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
import json
rows=[json.loads(line) for line in Path('data/learning_item_curation/learning_items.jsonl').read_text(encoding='utf-8').splitlines()]
print(len(rows))
print(rows[0]['item_code'], rows[0]['content'])
print(rows[-1]['item_code'], rows[-1]['content'])
print(sum(1 for r in rows if not r['meaning_zh']))
PY
```

Expected output:

```text
84243
w_000001 'd better
p_084243 your sincerely
16629
```

- [ ] **Step 4: Commit extraction tool**

Run:

```bash
git add tools/learning_item_curation/extract_learning_items.py
git commit -m "chore: add learning item extraction tool"
```

---

### Task 2: Generate Conservative Curation Candidates

**Files:**
- Create: `tools/learning_item_curation/generate_candidates.py`
- Output: `data/learning_item_curation/delete_candidates.jsonl`
- Output: `data/learning_item_curation/type_change_candidates.jsonl`
- Output: `data/learning_item_curation/meaning_batches/batch_0001.json` and later batches

- [ ] **Step 1: Create candidate generator**

Use `apply_patch` to create `tools/learning_item_curation/generate_candidates.py`:

```python
#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / "data/learning_item_curation"
INPUT = DATA_DIR / "learning_items.jsonl"
DELETE_OUTPUT = DATA_DIR / "delete_candidates.jsonl"
TYPE_OUTPUT = DATA_DIR / "type_change_candidates.jsonl"
BATCH_DIR = DATA_DIR / "meaning_batches"
BATCH_SIZE = 20

ORDINAL_RE = re.compile(r"^\d+(st|nd|rd|th)$", re.IGNORECASE)
LETTER_RE = re.compile(r"^[A-Za-z]$")
CONTRACTION_FRAGMENTS = {"'d better", "'d like", "'d like to", "'d rather", "'em", "'tis"}
COMMERCIAL_PERCENT_RE = re.compile(r"^\d+%\s+(discount|off)$", re.IGNORECASE)


def read_rows() -> list[dict[str, object]]:
    return [json.loads(line) for line in INPUT.read_text(encoding="utf-8").splitlines()]


def deletion_reason(row: dict[str, object]) -> str | None:
    if row["type"] != "WORD":
        return None
    content = str(row["content"])
    if ORDINAL_RE.fullmatch(content):
        return "pure ordinal number"
    if LETTER_RE.fullmatch(content):
        return "single letter/article as word card"
    if content in CONTRACTION_FRAGMENTS:
        return "contraction/fragment better learned in phrases"
    if COMMERCIAL_PERCENT_RE.fullmatch(content):
        return "commercial percentage phrase, not standalone word"
    return None


def type_change_reason(row: dict[str, object]) -> str | None:
    if row["type"] != "WORD":
        return None
    content = str(row["content"])
    if deletion_reason(row):
        return None
    if " " in content:
        return "contains spaces and behaves like phrase/collocation"
    return None


def llm_input(row: dict[str, object]) -> dict[str, object]:
    return {
        "item_code": row["item_code"],
        "type": row["type"],
        "content": row["content"],
        "theme": row["theme"],
        "tags": row["tags"],
        "difficulty": row["difficulty"],
        "examples": row["examples"],
    }


def write_jsonl(path: Path, rows: list[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8") as out:
        for row in rows:
            out.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")


def main() -> None:
    rows = read_rows()
    delete_rows = []
    delete_codes = set()
    for row in rows:
        reason = deletion_reason(row)
        if reason:
            delete_codes.add(row["item_code"])
            delete_rows.append({**row, "curation_reason": reason})
    type_rows = []
    for row in rows:
        reason = type_change_reason(row)
        if reason:
            type_rows.append({**row, "curation_reason": reason, "normalized_type": "PHRASE"})
    meaning_rows = [
        llm_input(row)
        for row in rows
        if not row["meaning_zh"] and row["item_code"] not in delete_codes
    ]

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    BATCH_DIR.mkdir(parents=True, exist_ok=True)
    write_jsonl(DELETE_OUTPUT, delete_rows)
    write_jsonl(TYPE_OUTPUT, type_rows)
    for old in BATCH_DIR.glob("batch_*.json"):
        old.unlink()
    for index in range(0, len(meaning_rows), BATCH_SIZE):
        batch = meaning_rows[index : index + BATCH_SIZE]
        path = BATCH_DIR / f"batch_{index // BATCH_SIZE + 1:04d}.json"
        path.write_text(json.dumps(batch, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"delete_candidates {len(delete_rows)}")
    print(f"type_change_candidates {len(type_rows)}")
    print(f"meaning_items {len(meaning_rows)}")
    print(f"meaning_batches {(len(meaning_rows) + BATCH_SIZE - 1) // BATCH_SIZE}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Generate candidate files**

Run:

```bash
python3 tools/learning_item_curation/generate_candidates.py
```

Expected approximate output based on the current data:

```text
delete_candidates 51
type_change_candidates 38413
meaning_items 16608
meaning_batches 831
```

If numbers differ, inspect why before continuing. They may differ only if previous tasks changed SQL.

- [ ] **Step 3: Review deletion candidates**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
import json
rows=[json.loads(line) for line in Path('data/learning_item_curation/delete_candidates.jsonl').read_text(encoding='utf-8').splitlines()]
for row in rows:
    print(row['item_code'], row['content'], row['curation_reason'], row['meaning_zh'] or '<empty>')
PY
```

Expected: includes `'d better`, `'d like`, `10th`, `23rd`, `a`, single letters, and `50% off`.

- [ ] **Step 4: Review type-change sample**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
import json
rows=[json.loads(line) for line in Path('data/learning_item_curation/type_change_candidates.jsonl').read_text(encoding='utf-8').splitlines()]
for row in rows[:100]:
    print(row['item_code'], row['content'], row['type'], '->', row['normalized_type'], row['meaning_zh'] or '<empty>')
PY
```

Expected: phrase-like `WORD` rows such as `'to-do' list`, `100-meter race`, `a bag of flour`, and `a better understanding of`.

- [ ] **Step 5: Commit candidate generator**

Run:

```bash
git add tools/learning_item_curation/generate_candidates.py
git commit -m "chore: generate learning item curation candidates"
```

---

### Task 3: Add LLM Prompt and Result Validator

**Files:**
- Create: `tools/learning_item_curation/prompts/meaning_enrichment_prompt.md`
- Create: `tools/learning_item_curation/validate_llm_results.py`
- Input: `data/learning_item_curation/meaning_results/*.json`
- Output: `data/learning_item_curation/accepted_meanings.jsonl`
- Output: `data/learning_item_curation/review_meanings.jsonl`

- [ ] **Step 1: Create the production prompt file**

Use `apply_patch` to create `tools/learning_item_curation/prompts/meaning_enrichment_prompt.md`:

```markdown
# Meaning Enrichment Prompt

## System Prompt

你是一名专业英语词汇数据编辑，负责为英语学习 App 的词库补充中文释义。

你的任务不是逐字翻译，而是为中国学习者提供准确、简洁、适合记忆卡片展示的中文释义。你必须根据词条本身、类型、主题、难度、标签和例句判断含义。

工作原则：
1. 优先给出该词条在英语学习场景中最常见、最有教学价值的中文释义。
2. 如果词条是短语、搭配、固定表达，应按整体含义解释，不要逐词硬翻。
3. 如果词条当前 type 是 WORD 但内容明显是短语或搭配，应建议 normalized_type 为 PHRASE。
4. 如果词条是低价值学习项，例如单个字母、纯序数词、缩写残片、明显噪声，应 should_keep=false。
5. 如果词条是专有名词、品牌、地名、人名、缩写或技术术语，应如实标注，不要编造普通含义。
6. 如果无法可靠判断含义，meaning_zh 设为 null，confidence 设为 low，并说明原因。
7. 中文释义使用简体中文，简洁自然，不要使用繁体。
8. 不要输出 Markdown，不要输出解释性段落，只输出严格 JSON。
9. 不要保留“n.”、“v.”、“adj.”等词性前缀，除非不加会造成歧义。
10. 多个常见义项用中文分号“；”分隔，通常不超过 3 个义项。

## User Prompt Template

请为下面这些 learning_item 补充或审核中文释义。

输出要求：
返回一个 JSON 数组。数组长度必须与输入条目数量一致，并且每个对象必须包含以下字段：

- item_code: 原样返回
- content: 原样返回
- meaning_zh: 适合写入数据库的中文释义；无法可靠判断时为 null
- normalized_type: "WORD" 或 "PHRASE"
- should_keep: true 或 false
- confidence: "high"、"medium" 或 "low"
- reason: 简短说明判断依据，最多 30 个中文字符

释义格式要求：
- meaning_zh 使用简体中文。
- 不要输出英文释义。
- 不要输出 Markdown。
- 不要额外输出任何 JSON 之外的文本。
- 如果 examples 能帮助判断含义，应优先参考 examples。
- 如果 examples 与 content 明显不匹配，应以 content 为准，并在 reason 中说明“例句不匹配”。

输入数据：
{{BATCH_JSON}}
```

- [ ] **Step 2: Create result validator**

Use `apply_patch` to create `tools/learning_item_curation/validate_llm_results.py`:

```python
#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / "data/learning_item_curation"
BATCH_DIR = DATA_DIR / "meaning_batches"
RESULT_DIR = DATA_DIR / "meaning_results"
ACCEPTED = DATA_DIR / "accepted_meanings.jsonl"
REVIEW = DATA_DIR / "review_meanings.jsonl"

REQUIRED_KEYS = {"item_code", "content", "meaning_zh", "normalized_type", "should_keep", "confidence", "reason"}
VALID_TYPES = {"WORD", "PHRASE"}
VALID_CONFIDENCE = {"high", "medium", "low"}


def load_batch_index() -> dict[str, dict[str, object]]:
    index: dict[str, dict[str, object]] = {}
    for path in sorted(BATCH_DIR.glob("batch_*.json")):
        for row in json.loads(path.read_text(encoding="utf-8")):
            index[str(row["item_code"])] = row
    return index


def validate_item(item: dict[str, object], source: dict[str, object], path: Path) -> str | None:
    missing = REQUIRED_KEYS - set(item)
    if missing:
        return f"{path.name}: missing keys {sorted(missing)}"
    if item["item_code"] != source["item_code"]:
        return f"{path.name}: item_code mismatch {item['item_code']}"
    if item["content"] != source["content"]:
        return f"{path.name}: content mismatch for {item['item_code']}"
    if item["normalized_type"] not in VALID_TYPES:
        return f"{path.name}: invalid normalized_type for {item['item_code']}"
    if item["confidence"] not in VALID_CONFIDENCE:
        return f"{path.name}: invalid confidence for {item['item_code']}"
    if not isinstance(item["should_keep"], bool):
        return f"{path.name}: should_keep is not bool for {item['item_code']}"
    if item["meaning_zh"] is not None and not isinstance(item["meaning_zh"], str):
        return f"{path.name}: meaning_zh must be string or null for {item['item_code']}"
    if isinstance(item["meaning_zh"], str) and not item["meaning_zh"].strip():
        return f"{path.name}: meaning_zh is blank for {item['item_code']}"
    if not isinstance(item["reason"], str) or not item["reason"].strip():
        return f"{path.name}: reason missing for {item['item_code']}"
    return None


def main() -> None:
    batch_index = load_batch_index()
    accepted: list[dict[str, object]] = []
    review: list[dict[str, object]] = []
    seen: set[str] = set()
    errors: list[str] = []

    RESULT_DIR.mkdir(parents=True, exist_ok=True)
    for path in sorted(RESULT_DIR.glob("batch_*.json")):
        items = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(items, list):
            errors.append(f"{path.name}: root is not a JSON array")
            continue
        for item in items:
            if not isinstance(item, dict):
                errors.append(f"{path.name}: array contains non-object")
                continue
            code = str(item.get("item_code", ""))
            source = batch_index.get(code)
            if source is None:
                errors.append(f"{path.name}: unknown item_code {code}")
                continue
            err = validate_item(item, source, path)
            if err:
                errors.append(err)
                continue
            if code in seen:
                errors.append(f"{path.name}: duplicate result for {code}")
                continue
            seen.add(code)
            enriched = {**source, **item}
            if item["should_keep"] is True and item["meaning_zh"] and item["confidence"] == "high":
                accepted.append(enriched)
            else:
                review.append(enriched)

    if errors:
        for error in errors:
            print(error)
        raise SystemExit(1)

    with ACCEPTED.open("w", encoding="utf-8") as out:
        for row in accepted:
            out.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
    with REVIEW.open("w", encoding="utf-8") as out:
        for row in review:
            out.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
    print(f"accepted {len(accepted)}")
    print(f"review {len(review)}")
    print(f"seen {len(seen)}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 3: Create result directory placeholder**

Run:

```bash
mkdir -p data/learning_item_curation/meaning_results
touch data/learning_item_curation/meaning_results/.gitkeep
```

- [ ] **Step 4: Commit prompt and validator**

Run:

```bash
git add tools/learning_item_curation/prompts/meaning_enrichment_prompt.md tools/learning_item_curation/validate_llm_results.py data/learning_item_curation/meaning_results/.gitkeep
git commit -m "chore: add meaning enrichment prompt and validator"
```

---

### Task 4: Produce LLM Meaning Results in Small Batches

**Files:**
- Input: `data/learning_item_curation/meaning_batches/batch_*.json`
- Output: `data/learning_item_curation/meaning_results/batch_*.json`

- [ ] **Step 1: Generate batches if not already generated**

Run:

```bash
python3 tools/learning_item_curation/extract_learning_items.py
python3 tools/learning_item_curation/generate_candidates.py
```

Expected: `data/learning_item_curation/meaning_batches/batch_0001.json` exists and contains at most 20 records.

- [ ] **Step 2: Run a pilot batch through the LLM**

Use `tools/learning_item_curation/prompts/meaning_enrichment_prompt.md` and replace `{{BATCH_JSON}}` with the exact contents of `data/learning_item_curation/meaning_batches/batch_0001.json`.

Save the raw JSON array returned by the LLM to:

```text
data/learning_item_curation/meaning_results/batch_0001.json
```

The file must contain only a JSON array. It must not contain Markdown fences or prose.

- [ ] **Step 3: Validate the pilot batch**

Run:

```bash
python3 tools/learning_item_curation/validate_llm_results.py
```

Expected output for one completed batch:

```text
accepted <number between 0 and 20>
review <number between 0 and 20>
seen 20
```

If validation fails, fix the JSON result file rather than changing SQL.

- [ ] **Step 4: Inspect pilot quality before continuing**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
import json
for path in ['data/learning_item_curation/accepted_meanings.jsonl','data/learning_item_curation/review_meanings.jsonl']:
    print(path)
    if not Path(path).exists():
        continue
    for line in Path(path).read_text(encoding='utf-8').splitlines()[:20]:
        row=json.loads(line)
        print(row['item_code'], row['content'], row.get('meaning_zh'), row.get('normalized_type'), row.get('confidence'), row.get('reason'))
PY
```

Expected: meanings are concise Simplified Chinese and phrase-like entries use `PHRASE`.

- [ ] **Step 5: Continue batches incrementally**

Process `batch_0002.json` through `batch_0831.json` in order. After every 10 batches, run:

```bash
python3 tools/learning_item_curation/validate_llm_results.py
```

Expected: no validation errors. The `seen` count should equal `completed_batch_count * 20`, except the final batch can contain fewer than 20.

- [ ] **Step 6: Commit accepted LLM results only after validation**

Run:

```bash
git add data/learning_item_curation/meaning_results data/learning_item_curation/accepted_meanings.jsonl data/learning_item_curation/review_meanings.jsonl
git commit -m "data: add validated learning item meaning results"
```

If the result files are too large to commit, skip this commit and document the reason in the final handoff. Do not commit partial unvalidated results.

---

### Task 5: Apply Deletions, Type Changes, and Accepted Meanings to SQL

**Files:**
- Create: `tools/learning_item_curation/apply_curation.py`
- Modify: `java-core/src/main/resources/db/migration/V5__seed_vocabulary_part1.sql` through `java-core/src/main/resources/db/migration/V21__seed_vocabulary_part17.sql`

- [ ] **Step 1: Create SQL rewriter**

Use `apply_patch` to create `tools/learning_item_curation/apply_curation.py`:

```python
#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MIGRATION_DIR = ROOT / "java-core/src/main/resources/db/migration"
DATA_DIR = ROOT / "data/learning_item_curation"
DELETE_INPUT = DATA_DIR / "delete_candidates.jsonl"
TYPE_INPUT = DATA_DIR / "type_change_candidates.jsonl"
MEANING_INPUT = DATA_DIR / "accepted_meanings.jsonl"


def split_sql_values(row: str) -> list[str]:
    fields: list[str] = []
    current: list[str] = []
    in_string = False
    i = 0
    while i < len(row):
        ch = row[i]
        if in_string:
            current.append(ch)
            if ch == "'":
                if i + 1 < len(row) and row[i + 1] == "'":
                    current.append(row[i + 1])
                    i += 2
                    continue
                in_string = False
            i += 1
            continue
        if ch == "'":
            in_string = True
            current.append(ch)
            i += 1
            continue
        if ch == ",":
            fields.append("".join(current).strip())
            current = []
            i += 1
            if i < len(row) and row[i] == " ":
                i += 1
            continue
        current.append(ch)
        i += 1
    fields.append("".join(current).strip())
    return fields


def unquote(value: str) -> str:
    return value[1:-1].replace("''", "'")


def quote(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def read_jsonl(path: Path) -> list[dict[str, object]]:
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def migration_version(path: Path) -> int:
    match = re.match(r"V(\d+)__", path.name)
    if not match:
        raise ValueError(f"Unexpected migration filename: {path.name}")
    return int(match.group(1))


def rewrite_file(path: Path, delete_codes: set[str], type_updates: dict[str, str], meaning_updates: dict[str, str]) -> tuple[int, int, int]:
    lines = path.read_text(encoding="utf-8").splitlines()
    rows: list[str] = []
    deleted = 0
    typed = 0
    meant = 0
    for line in lines:
        if not line.startswith("('"):
            rows.append(line)
            continue
        terminator = line[-2:]
        fields = split_sql_values(line[1:-2])
        code = unquote(fields[0])
        if code in delete_codes:
            deleted += 1
            continue
        if code in type_updates and fields[1] != quote(type_updates[code]):
            fields[1] = quote(type_updates[code])
            typed += 1
        if code in meaning_updates and fields[3] != quote(meaning_updates[code]):
            fields[3] = quote(meaning_updates[code])
            meant += 1
        rows.append("(" + ", ".join(fields) + terminator)

    # Fix row separators after deletion so only the final INSERT row ends with );.
    row_indexes = [i for i, line in enumerate(rows) if line.startswith("('")]
    for i in row_indexes:
        if rows[i].endswith(");"):
            rows[i] = rows[i][:-2] + "),"
    if row_indexes:
        last = row_indexes[-1]
        if rows[last].endswith("),"):
            rows[last] = rows[last][:-2] + ");"
    path.write_text("\n".join(rows) + "\n", encoding="utf-8")
    return deleted, typed, meant


def main() -> None:
    delete_codes = {str(row["item_code"]) for row in read_jsonl(DELETE_INPUT)}
    type_updates = {
        str(row["item_code"]): str(row["normalized_type"])
        for row in read_jsonl(TYPE_INPUT)
        if row.get("normalized_type") == "PHRASE"
    }
    meaning_updates = {
        str(row["item_code"]): str(row["meaning_zh"])
        for row in read_jsonl(MEANING_INPUT)
        if row.get("should_keep") is True and row.get("meaning_zh")
    }
    totals = [0, 0, 0]
    for path in sorted(MIGRATION_DIR.glob("V*.sql"), key=migration_version):
        if migration_version(path) < 5:
            continue
        deleted, typed, meant = rewrite_file(path, delete_codes, type_updates, meaning_updates)
        totals[0] += deleted
        totals[1] += typed
        totals[2] += meant
    print(f"deleted {totals[0]}")
    print(f"type_changed {totals[1]}")
    print(f"meaning_updated {totals[2]}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Apply curation changes**

Run:

```bash
python3 tools/learning_item_curation/apply_curation.py
```

Expected output after all LLM batches are done:

```text
deleted 51
type_changed <number based on accepted type policy>
meaning_updated <number of accepted high-confidence meanings>
```

If LLM batches are not complete yet, `meaning_updated` will be lower. Do not proceed to final verification until the intended batch scope is complete.

- [ ] **Step 3: Re-extract and verify counts**

Run:

```bash
python3 tools/learning_item_curation/extract_learning_items.py
python3 - <<'PY'
from pathlib import Path
import json
rows=[json.loads(line) for line in Path('data/learning_item_curation/learning_items.jsonl').read_text(encoding='utf-8').splitlines()]
print('rows', len(rows))
print('empty_meaning', sum(1 for r in rows if not r['meaning_zh']))
print('word_with_space', sum(1 for r in rows if r['type']=='WORD' and ' ' in r['content']))
print('phrase_count', sum(1 for r in rows if r['type']=='PHRASE'))
print('bad_deleted_present', [r['content'] for r in rows if r['content'] in {"'d better", "10th", "23rd", "a"}])
PY
```

Expected after full application:

```text
rows 84192
empty_meaning <lower than 16629>
word_with_space <lower than previous count>
phrase_count <higher than 1936>
bad_deleted_present []
```

- [ ] **Step 4: Commit applied SQL changes**

Run:

```bash
git add java-core/src/main/resources/db/migration tools/learning_item_curation/apply_curation.py
git commit -m "data: curate learning item seed data"
```

---

### Task 6: Verify Flyway and Project Build

**Files:**
- Verify: `java-core/src/main/resources/db/migration/*.sql`
- Verify: `java-core/target/surefire-reports/TEST-*.xml`

- [ ] **Step 1: Check migration file sequence**

Run:

```bash
find java-core/src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' -printf '%f\n' | sort -V
```

Expected: `V1` through `V21`, with `V1-V4` schema migrations and `V5-V21` seed migrations.

- [ ] **Step 2: Check no update migrations returned**

Run:

```bash
rg -n "^UPDATE learning_item|enrich_examples_with_tatoeba" java-core/src/main/resources/db/migration || true
```

Expected: no output.

- [ ] **Step 3: Run full Maven verification**

Run:

```bash
cd java-core && mvn -q clean package
```

Expected: command exits `0`. Flyway logs should say `Successfully validated 21 migrations` and `Successfully applied 21 migrations ... now at version v21`.

- [ ] **Step 4: Summarize test report**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
reports=sorted(Path('java-core/target/surefire-reports').glob('TEST-*.xml'))
tests=failures=errors=skipped=0
for p in reports:
    root=ET.parse(p).getroot()
    tests += int(root.attrib.get('tests', 0))
    failures += int(root.attrib.get('failures', 0))
    errors += int(root.attrib.get('errors', 0))
    skipped += int(root.attrib.get('skipped', 0))
print('reports', len(reports))
print('tests', tests)
print('failures', failures)
print('errors', errors)
print('skipped', skipped)
PY
```

Expected: `failures 0` and `errors 0`.

- [ ] **Step 5: Commit verification-only updates if any**

If running tools changed only generated audit files that should not be committed, do not commit them. If a committed report is desired, add it explicitly with:

```bash
git add data/learning_item_curation
git commit -m "data: add learning item curation audit artifacts"
```

---

## Completion Audit Checklist

- [ ] Deletion requirement: `delete_candidates.jsonl` contains the agreed low-value rows, and final SQL no longer contains those `item_code`s.
- [ ] Meaning completion requirement: every LLM output applied to SQL passed `validate_llm_results.py`; remaining empty meanings are either intentionally unprocessed or listed in `review_meanings.jsonl`.
- [ ] Type change requirement: phrase-like accepted rows are changed from `WORD` to `PHRASE`; remaining `WORD` rows with spaces are reviewed or intentionally deferred.
- [ ] Flyway requirement: source migration versions remain continuous from `V1` to `V21`.
- [ ] Build requirement: `cd java-core && mvn -q clean package` exits `0`.
- [ ] Git requirement: changes are committed and ready to push.

