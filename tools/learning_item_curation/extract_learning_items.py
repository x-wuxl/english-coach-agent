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
