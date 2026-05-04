#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_DIR="${SMOKE_ARTIFACT_DIR:-tools/smoke-artifacts}"
LAST_FLOW="$ARTIFACT_DIR/learning-flow-last.json"

DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-english_coach}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-123456}"
USER_ID="${USER_ID:-}"

need() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing required command: $1" >&2
        exit 127
    }
}

need psql
need jq

if [[ -z "$USER_ID" && -f "$LAST_FLOW" ]]; then
    USER_ID="$(jq -r '.userId // empty' "$LAST_FLOW")"
fi
[[ -n "$USER_ID" && "$USER_ID" != "null" ]] || {
    echo "USER_ID is required, or run tools/smoke-learning-flow.sh first" >&2
    exit 2
}

query_count() {
    local sql="$1"
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tA -c "$sql"
}

assert_min() {
    local label="$1"
    local value="$2"
    local min="$3"
    [[ "$value" =~ ^[0-9]+$ ]] || {
        echo "DB CHECK FAILED: $label returned non-numeric value: $value" >&2
        exit 1
    }
    if (( value < min )); then
        echo "DB CHECK FAILED: $label=$value, expected >= $min" >&2
        exit 1
    fi
    echo "$label=$value"
}

echo "DB smoke config: host=$DB_HOST port=$DB_PORT db=$DB_NAME user=$DB_USER learnerUserId=$USER_ID"

learning_items="$(query_count 'select count(*) from learning_item;')"
plan_count="$(query_count "select count(*) from daily_plan_snapshot where user_id = $USER_ID;")"
plan_item_count="$(query_count "select count(*) from daily_plan_item dpi join daily_plan_snapshot dps on dpi.daily_plan_snapshot_id = dps.id where dps.user_id = $USER_ID;")"
completed_session_count="$(query_count "select count(*) from study_session where user_id = $USER_ID and status = 'COMPLETED';")"
attempt_count="$(query_count "select count(*) from attempt_log where user_id = $USER_ID;")"
mastery_count="$(query_count "select count(*) from mastery_state where user_id = $USER_ID;")"

assert_min learning_item_count "$learning_items" 1
assert_min daily_plan_snapshot_count "$plan_count" 1
assert_min daily_plan_item_count "$plan_item_count" 1
assert_min completed_study_session_count "$completed_session_count" 1
assert_min attempt_log_count "$attempt_count" 1
assert_min mastery_state_count "$mastery_count" 1

if (( learning_items < 80000 )); then
    echo "DB CHECK WARNING: learning_item_count=$learning_items is below the expected production-like 80000+ rows" >&2
fi

echo "DB CHECK PASSED userId=$USER_ID"
