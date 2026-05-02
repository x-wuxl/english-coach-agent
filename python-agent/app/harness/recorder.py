import json
import logging
import os
from datetime import datetime

from app.config import settings

logger = logging.getLogger(__name__)

RECORD_DIR = settings.harness_record_dir


def record_interaction(trace_id: str, request_type: str, request_data: dict, response_data: dict):
    """Record an API interaction to a JSON file for replay."""
    os.makedirs(RECORD_DIR, exist_ok=True)

    record = {
        "trace_id": trace_id,
        "timestamp": datetime.now().isoformat(),
        "request_type": request_type,
        "request": request_data,
        "response": response_data,
    }

    filename = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{trace_id}.json"
    filepath = os.path.join(RECORD_DIR, filename)

    try:
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(record, f, ensure_ascii=False, indent=2)
        logger.info("Recorded interaction to %s", filepath)
    except Exception as e:
        logger.warning("Failed to record interaction: %s", e)
