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
