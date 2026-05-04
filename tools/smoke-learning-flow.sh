#!/usr/bin/env bash
set -euo pipefail

JAVA_BASE="${JAVA_BASE:-http://localhost:8080}"
PYTHON_BASE="${PYTHON_BASE:-http://localhost:8000}"
USER_CODE="${USER_CODE:-smoke_learning_$(date +%Y%m%d%H%M%S)}"
PLAN_DATE="${PLAN_DATE:-$(date +%F)}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
ARTIFACT_DIR="${SMOKE_ARTIFACT_DIR:-tools/smoke-artifacts}"
mkdir -p "$ARTIFACT_DIR"

need() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing required command: $1" >&2
        exit 127
    }
}

need curl
need jq

last_body="$TMP_DIR/last-body.json"

fail() {
    echo "SMOKE FAILED: $*" >&2
    if [[ -s "$last_body" ]]; then
        echo "Last response body:" >&2
        cat "$last_body" >&2
        echo >&2
    fi
    exit 1
}

request_json() {
    local method="$1"
    local url="$2"
    local body="${3:-}"
    local out="$TMP_DIR/response-$(date +%s%N).txt"
    local status

    echo "> $method $url" >&2
    if [[ -n "$body" ]]; then
        if ! curl -sS -X "$method" "$url" \
            -H 'Content-Type: application/json' \
            -d "$body" \
            -o "$last_body" \
            -w '%{http_code}' >"$out"; then
            fail "curl failed for $method $url"
        fi
    else
        if ! curl -sS -X "$method" "$url" \
            -o "$last_body" \
            -w '%{http_code}' >"$out"; then
            fail "curl failed for $method $url"
        fi
    fi

    status="$(cat "$out")"
    [[ "$status" =~ ^2[0-9][0-9]$ ]] || fail "$method $url returned HTTP $status"
    jq empty "$last_body" >/dev/null || fail "$method $url returned non-JSON"
    cat "$last_body"
}

assert_base_ok() {
    local json="$1"
    local label="$2"
    local code
    code="$(jq -r '.code // empty' <<<"$json")"
    [[ "$code" == "0" ]] || fail "$label returned business code ${code:-missing}"
}

echo "Smoke config: JAVA_BASE=$JAVA_BASE PYTHON_BASE=$PYTHON_BASE USER_CODE=$USER_CODE PLAN_DATE=$PLAN_DATE" >&2

python_health="$(request_json GET "$PYTHON_BASE/health")"
[[ "$(jq -r '.status // empty' <<<"$python_health")" == "ok" ]] || fail "Python health did not return status=ok"

java_health="$(request_json GET "$JAVA_BASE/api/health")"
assert_base_ok "$java_health" "Java health"

user_lookup="$(request_json GET "$JAVA_BASE/api/users/by-code/$USER_CODE")"
lookup_code="$(jq -r '.code // empty' <<<"$user_lookup")"
if [[ "$lookup_code" == "0" ]]; then
    user_json="$user_lookup"
else
    user_json="$(request_json POST "$JAVA_BASE/api/users" "$(jq -nc --arg code "$USER_CODE" '{userCode:$code,goal:"GENERAL",subGoals:["daily"],dailyMinutes:20,preferredModes:["recognition_quiz","cn_to_en"],motivationStyle:"DIRECT",fatigueTolerance:"MEDIUM"}')")"
    assert_base_ok "$user_json" "Create user"
fi
user_id="$(jq -r '.data.id // empty' <<<"$user_json")"
[[ -n "$user_id" && "$user_id" != "null" ]] || fail "User id is missing"

plan_json="$(request_json POST "$JAVA_BASE/api/plans/daily:ensure" "$(jq -nc --argjson userId "$user_id" --arg planDate "$PLAN_DATE" '{userId:$userId,planDate:$planDate,planType:"NORMAL"}')")"
assert_base_ok "$plan_json" "Ensure daily plan"

plan_item_count="$(jq '[.data.newItems[]?, .data.reviewItems[]?] | length' <<<"$plan_json")"
[[ "$plan_item_count" -ge 1 ]] || fail "Daily plan has no items"

learning_item_id="$(jq -r '[.data.newItems[]?, .data.reviewItems[]?] | .[0].itemId // empty' <<<"$plan_json")"
[[ -n "$learning_item_id" && "$learning_item_id" != "null" ]] || fail "Daily plan first item has no itemId"

session_json="$(request_json POST "$JAVA_BASE/api/sessions/start" "$(jq -nc --argjson userId "$user_id" '{userId:$userId,sessionType:"DAILY_LEARNING",focusTheme:"daily"}')")"
assert_base_ok "$session_json" "Start study session"
session_id="$(jq -r '.data.id // empty' <<<"$session_json")"
[[ -n "$session_id" && "$session_id" != "null" ]] || fail "Study session id is missing"

attempt_json="$(request_json POST "$JAVA_BASE/api/sessions/$session_id/attempts" "$(jq -nc --argjson itemId "$learning_item_id" '{attempts:[{learningItemId:$itemId,mode:"recognition_quiz",result:"CORRECT",responseText:"I know it",responseTimeMs:1000,hintUsed:false}]}')")"
assert_base_ok "$attempt_json" "Submit attempt"

complete_json="$(request_json POST "$JAVA_BASE/api/sessions/$session_id/complete" '{"moodFeedback":"ok","fatigueFeedback":"LOW"}')"
assert_base_ok "$complete_json" "Complete study session"

mastery_json="$(request_json GET "$JAVA_BASE/api/mastery?userId=$user_id")"
assert_base_ok "$mastery_json" "Query mastery"
mastery_count="$(jq --argjson itemId "$learning_item_id" '[.data.items[]? | select(.learningItemId == $itemId)] | length' <<<"$mastery_json")"
[[ "$mastery_count" -ge 1 ]] || fail "Mastery does not contain learning item $learning_item_id"

memory_json="$(request_json GET "$JAVA_BASE/api/memory/priority?userId=$user_id")"
assert_base_ok "$memory_json" "Query priority memory"
jq -e '.data.items | type == "array"' <<<"$memory_json" >/dev/null || fail "Memory response does not contain data.items array"

echo "SMOKE PASSED userId=$user_id sessionId=$session_id learningItemId=$learning_item_id"
jq -nc --argjson userId "$user_id" --argjson sessionId "$session_id" --argjson learningItemId "$learning_item_id" \
    --arg userCode "$USER_CODE" --arg planDate "$PLAN_DATE" \
    '{userId:$userId, sessionId:$sessionId, learningItemId:$learningItemId, userCode:$userCode, planDate:$planDate}' \
    > "$ARTIFACT_DIR/learning-flow-last.json"
echo "Wrote $ARTIFACT_DIR/learning-flow-last.json"
