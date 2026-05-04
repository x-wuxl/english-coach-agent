#!/usr/bin/env bash
set -euo pipefail

JAVA_BASE="${JAVA_BASE:-http://localhost:8080}"
PYTHON_BASE="${PYTHON_BASE:-http://localhost:8000}"
PLAN_DATE="${PLAN_DATE:-$(date +%F)}"
RUN_ID="$(date +%Y%m%d%H%M%S)"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

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
    echo "API MATRIX FAILED: $*" >&2
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
    local expected_http="${4:-2xx}"
    local status_file="$TMP_DIR/status-$(date +%s%N).txt"
    echo "> $method $url" >&2
    if [[ -n "$body" ]]; then
        curl -sS -X "$method" "$url" -H 'Content-Type: application/json' -d "$body" -o "$last_body" -w '%{http_code}' >"$status_file" \
            || fail "curl failed for $method $url"
    else
        curl -sS -X "$method" "$url" -o "$last_body" -w '%{http_code}' >"$status_file" \
            || fail "curl failed for $method $url"
    fi
    local status
    status="$(cat "$status_file")"
    if [[ "$expected_http" == "2xx" ]]; then
        [[ "$status" =~ ^2[0-9][0-9]$ ]] || fail "$method $url returned HTTP $status"
    else
        [[ "$status" == "$expected_http" ]] || fail "$method $url returned HTTP $status, expected $expected_http"
    fi
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

assert_base_not_ok() {
    local json="$1"
    local label="$2"
    local code
    code="$(jq -r '.code // empty' <<<"$json")"
    [[ -n "$code" && "$code" != "0" ]] || fail "$label unexpectedly returned success"
}

create_user() {
    local code="$1"
    local json
    json="$(request_json POST "$JAVA_BASE/api/users" "$(jq -nc --arg code "$code" '{userCode:$code,goal:"GENERAL",subGoals:["daily"],dailyMinutes:20,preferredModes:["recognition_quiz","cn_to_en","sentence_building"],motivationStyle:"DIRECT",fatigueTolerance:"MEDIUM"}')")"
    assert_base_ok "$json" "create user $code"
    jq -r '.data.id' <<<"$json"
}

ensure_plan() {
    local user_id="$1"
    local date="$2"
    local json
    json="$(request_json POST "$JAVA_BASE/api/plans/daily:ensure" "$(jq -nc --argjson userId "$user_id" --arg planDate "$date" '{userId:$userId,planDate:$planDate,planType:"NORMAL"}')")"
    assert_base_ok "$json" "ensure daily plan"
    echo "$json"
}

submit_attempt_session() {
    local user_id="$1"
    local item_id="$2"
    local mode="$3"
    local result="$4"
    local response_text="$5"
    local session_json session_id attempt_json complete_json detail_json
    session_json="$(request_json POST "$JAVA_BASE/api/sessions/start" "$(jq -nc --argjson userId "$user_id" --arg mode "$mode" '{userId:$userId,sessionType:"DAILY_LEARNING",focusTheme:$mode}')")"
    assert_base_ok "$session_json" "start session for $mode"
    session_id="$(jq -r '.data.id' <<<"$session_json")"
    attempt_json="$(request_json POST "$JAVA_BASE/api/sessions/$session_id/attempts" "$(jq -nc --argjson itemId "$item_id" --arg mode "$mode" --arg result "$result" --arg response "$response_text" '{attempts:[{learningItemId:$itemId,mode:$mode,result:$result,responseText:$response,responseTimeMs:900,hintUsed:false,errorType:"SMOKE"}]}')")"
    assert_base_ok "$attempt_json" "submit $result attempt for $mode"
    complete_json="$(request_json POST "$JAVA_BASE/api/sessions/$session_id/complete" '{"moodFeedback":"ok","fatigueFeedback":"LOW"}')"
    assert_base_ok "$complete_json" "complete session for $mode"
    detail_json="$(request_json GET "$JAVA_BASE/api/sessions/$session_id")"
    assert_base_ok "$detail_json" "session detail for $mode"
    jq -e --arg mode "$mode" --arg result "$result" '.data.attempts[] | select(.mode == $mode and .result == $result)' <<<"$detail_json" >/dev/null \
        || fail "session detail does not contain $mode/$result attempt"
    echo "$session_id"
}

echo "API matrix config: JAVA_BASE=$JAVA_BASE PYTHON_BASE=$PYTHON_BASE PLAN_DATE=$PLAN_DATE"

python_health="$(request_json GET "$PYTHON_BASE/health")"
[[ "$(jq -r '.status // empty' <<<"$python_health")" == "ok" ]] || fail "Python health failed"
java_health="$(request_json GET "$JAVA_BASE/api/health")"
assert_base_ok "$java_health" "Java health"

user_code="matrix_${RUN_ID}"
user_id="$(create_user "$user_code")"

existing_user_json="$(request_json GET "$JAVA_BASE/api/users/by-code/$user_code")"
assert_base_ok "$existing_user_json" "load existing user by code"
[[ "$(jq -r '.data.id' <<<"$existing_user_json")" == "$user_id" ]] || fail "existing user id mismatch"

new_user_id="$(create_user "matrix_new_${RUN_ID}")"
[[ -n "$new_user_id" ]] || fail "new user was not created"

blank_user_json="$(request_json POST "$JAVA_BASE/api/users" '{"userCode":"","goal":"GENERAL","dailyMinutes":20}')"
assert_base_not_ok "$blank_user_json" "blank user code validation"

plan_json="$(ensure_plan "$user_id" "$PLAN_DATE")"
item_id="$(jq -r '[.data.newItems[]?, .data.reviewItems[]?] | .[0].itemId // empty' <<<"$plan_json")"
[[ -n "$item_id" && "$item_id" != "null" ]] || fail "plan did not return learning item id"

second_plan_json="$(ensure_plan "$user_id" "$PLAN_DATE")"
[[ "$(jq -r '.data.planCode' <<<"$plan_json")" == "$(jq -r '.data.planCode' <<<"$second_plan_json")" ]] \
    || fail "repeated daily:ensure returned different planCode"

invalid_user_plan="$(request_json POST "$JAVA_BASE/api/plans/daily:ensure" '{"userId":999999999,"planDate":"2026-05-04","planType":"NORMAL"}')"
assert_base_not_ok "$invalid_user_plan" "invalid user daily plan"

invalid_date_plan="$(request_json POST "$JAVA_BASE/api/plans/daily:ensure" "$(jq -nc --argjson userId "$user_id" '{userId:$userId,planDate:"not-a-date",planType:"NORMAL"}')")"
assert_base_not_ok "$invalid_date_plan" "invalid date daily plan"

submit_attempt_session "$user_id" "$item_id" "recognition_quiz" "CORRECT" "I know it" >/dev/null
submit_attempt_session "$user_id" "$item_id" "cn_to_en" "WRONG" "wrong answer" >/dev/null
sentence_session_id="$(submit_attempt_session "$user_id" "$item_id" "sentence_building" "CORRECT" "I need to prepare the demo.")"

duplicate_complete="$(request_json POST "$JAVA_BASE/api/sessions/$sentence_session_id/complete" '{}')"
assert_base_not_ok "$duplicate_complete" "duplicate session complete"

content_json="$(request_json GET "$JAVA_BASE/api/content/items?page=1&size=5")"
assert_base_ok "$content_json" "content list"
jq -e '.data.items | length >= 1' <<<"$content_json" >/dev/null || fail "content list returned no items"

mastery_json="$(request_json GET "$JAVA_BASE/api/mastery/due-review?userId=$user_id")"
assert_base_ok "$mastery_json" "due review mastery"
jq -e '.data.items | type == "array"' <<<"$mastery_json" >/dev/null || fail "due review items is not an array"

memory_json="$(request_json GET "$JAVA_BASE/api/memory/priority?userId=$user_id")"
assert_base_ok "$memory_json" "priority memory"
jq -e '.data.items | type == "array"' <<<"$memory_json" >/dev/null || fail "priority memory items is not an array"

progress_json="$(request_json GET "$JAVA_BASE/api/progress/summary?userId=$user_id")"
assert_base_ok "$progress_json" "progress summary"
jq -e '.data.totalMasteryItems >= 1 and (.data.topWeakItems | type == "array")' <<<"$progress_json" >/dev/null \
    || fail "progress summary did not include real mastery data"

coach_session_json="$(request_json POST "$JAVA_BASE/api/coach/sessions" "$(jq -nc --argjson userId "$user_id" '{userId:$userId,sessionType:"TODAY_COACH"}')")"
assert_base_ok "$coach_session_json" "start coach session"
coach_session_id="$(jq -r '.data.id' <<<"$coach_session_json")"

chat_json="$(request_json POST "$JAVA_BASE/api/coach/sessions/$coach_session_id/turns" '{"mode":"CHAT","message":"I want to talk about my daily plan."}')"
assert_base_ok "$chat_json" "coach CHAT turn"
jq -e '.data.coachReply | type == "string" and length > 0' <<<"$chat_json" >/dev/null || fail "CHAT did not return coachReply"

fix_json="$(request_json POST "$JAVA_BASE/api/coach/sessions/$coach_session_id/turns" '{"mode":"FIX","message":"I need prepare the demo."}')"
assert_base_ok "$fix_json" "coach FIX turn"
jq -e '.data.fixResponse.better_english | type == "string" and length > 0' <<<"$fix_json" >/dev/null || fail "FIX did not return fixResponse.better_english"

natural_fix_json="$(request_json POST "$JAVA_BASE/api/coach/sessions/$coach_session_id/turns" '{"mode":"FIX","message":"I need to prepare the demo."}')"
assert_base_ok "$natural_fix_json" "coach FIX natural sentence turn"
jq -e '.data.fixResponse.better_english | type == "string" and length > 0' <<<"$natural_fix_json" >/dev/null || fail "natural FIX did not return fixResponse"
natural_better="$(jq -r '.data.fixResponse.better_english' <<<"$natural_fix_json")"
[[ "$natural_better" != "I need to prepare the demo." ]] || fail "natural FIX repeated the same sentence as correction"

drill_json="$(request_json POST "$JAVA_BASE/api/coach/sessions/$coach_session_id/turns" '{"mode":"DRILL","message":"Give me one practice prompt."}')"
assert_base_ok "$drill_json" "coach DRILL turn"
jq -e '.data.coachReply | type == "string" and length > 0' <<<"$drill_json" >/dev/null || fail "DRILL did not return coachReply"

blank_turn_json="$(request_json POST "$JAVA_BASE/api/coach/sessions/$coach_session_id/turns" '{"mode":"CHAT","message":""}')"
assert_base_not_ok "$blank_turn_json" "blank coach message validation"

echo "API MATRIX PASSED userId=$user_id itemId=$item_id"
