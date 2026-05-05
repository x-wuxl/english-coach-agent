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
