import json
import logging
import os

from app.harness.recorder import RECORD_DIR

logger = logging.getLogger(__name__)


def list_recordings() -> list[dict]:
    """List all recorded interactions."""
    if not os.path.exists(RECORD_DIR):
        return []

    recordings = []
    for filename in sorted(os.listdir(RECORD_DIR)):
        if not filename.endswith(".json"):
            continue
        filepath = os.path.join(RECORD_DIR, filename)
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                record = json.load(f)
            recordings.append({
                "filename": filename,
                "trace_id": record.get("trace_id"),
                "timestamp": record.get("timestamp"),
                "request_type": record.get("request_type"),
            })
        except Exception as e:
            logger.warning("Failed to read %s: %s", filename, e)

    return recordings


def load_recording(filename: str) -> dict | None:
    """Load a specific recorded interaction."""
    filepath = os.path.join(RECORD_DIR, filename)
    if not os.path.exists(filepath):
        return None

    try:
        with open(filepath, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        logger.warning("Failed to load %s: %s", filename, e)
        return None
