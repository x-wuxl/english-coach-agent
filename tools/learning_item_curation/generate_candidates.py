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
