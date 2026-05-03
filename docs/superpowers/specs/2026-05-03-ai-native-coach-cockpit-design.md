# AI Native Coach Cockpit Design

Date: 2026-05-03
Status: draft
Input source: product brainstorming with user

## 1. Goal

Reposition the current English Coach demo from a module-based learning system into an AI-native coaching experience.

Current loop:

```text
Placement -> Study -> Dashboard -> Mastery -> Weekly Review
```

Target loop:

```text
First Coaching Session -> Today Coach -> Memory-driven Drill -> Coach Review
```

The first version should validate one strong learning loop:

```text
Free English conversation
-> AI saves lightweight notes
-> Memory prioritizes repeated problems and expression gaps
-> AI suggests short drills when patterns repeat
-> Inline drills strengthen the pattern
-> Conversation resumes
-> Review summarizes progress and next actions
```

## 2. Product Positioning

The product should be positioned as:

> An AI English Coach that starts from free conversation, notices the learner's real expression problems without constantly interrupting, and turns repeated errors and expression gaps into short drills and long-term memory.

The first version should avoid becoming a vocabulary app with AI feedback, a quiz platform with LLM grading, a generic chatbot with no learning memory, or a dashboard-heavy learning management system.

## 3. Confirmed Product Decisions

### 3.1 Main Entry

The primary entry is `Today Coach`, not `Dashboard`.

Existing modules are repositioned:

```text
Placement      -> First Coaching Session
Study          -> Today Coach + Mini Drill
Mastery        -> Memory
Weekly Review  -> Coach Review
Dashboard      -> Progress, secondary
```

### 3.2 Main Page

Use a `Coach Cockpit` layout:

```text
Today Coach
├─ top: coach brief
├─ left: coach conversation stream
├─ right: priority memory panel
└─ bottom: mode-aware input
```

The conversation stream remains primary. Memory and drills are visible enough to make the product feel personal, but they should not dominate the screen.

### 3.3 Modes

The input supports three modes:

```text
Chat  - free conversation, light correction only
Fix   - diagnostic correction for a specific sentence
Drill - short practice generated from memory
```

Mode switching happens inside the same page. Users should not navigate to a separate exercise module to practice a pattern from the current conversation.

### 3.4 Chat Correction Style

Chat mode uses lightweight correction notes. When the learner makes a notable mistake, the coach continues the conversation and adds a small note:

```text
Saved note: need to prepare · Expand
```

The detailed correction appears in the right panel or expanded note, not as a long interruption in the chat.

Example:

```text
User:
I need prepare the demo.

Coach:
What part of the demo feels hardest?

Saved note:
need to prepare · Expand
```

Expanded detail:

```text
Your sentence:
I need prepare the demo.

Better:
I need to prepare the demo.

Why:
After need, use need to + verb.
```

### 3.5 Drill Entry Rule

The AI should not interrupt the first time it detects a problem.

Recommended rule:

```text
First occurrence:
Save a note only.

Second occurrence of the same pattern:
Lightly suggest a 1-minute drill.

User accepts:
Insert an inline mini drill into the chat stream.

User declines:
Keep the note in priority memory and continue the conversation.
```

Example:

```text
Coach:
I noticed you missed "to" again after want/need. Want to practice this for 1 minute?

[Start 1-min Drill] [Not now]
```

### 3.6 Drill Presentation

Mini drills are inserted inline in the chat stream.

Example:

```text
Mini Drill 1 / 2

Translate into English:
我需要准备开场白。
```

After the drill, the coach returns to the original conversation topic:

```text
Good. Now back to your demo. What is your opening sentence?
```

### 3.7 Fix Mode

Fix mode is diagnostic correction, not generic polishing. The response should have a stable five-part shape:

```text
1. Meaning Check
2. Better English
3. What Changed
4. Memory Update
5. Try Again
```

Example:

```text
Meaning:
You want to say: 我需要准备这个 demo。

Better:
I need to prepare the demo.

What changed:
After need, use need to + verb.

Memory:
I saved "need to + verb" for future practice.

Try again:
Say: 我想提高我的开场白。
```

### 3.8 Learner Memory v1

Memory v1 stores two types of learning material:

```text
Error Patterns
Expression Gaps
```

Error patterns record repeated structural or usage problems:

```text
pattern_key: missing_infinitive_to
label: need/want/have time + to + verb
user_examples:
- I need prepare the demo.
- I want improve my opening.
better_examples:
- I need to prepare the demo.
- I want to improve my opening.
```

Expression gaps record meanings the user wanted to express but could not express naturally:

```text
zh_intent: 我来不及了
natural_expressions:
- I'm running late.
- I won't make it on time.
user_attempts:
- I have no time.
- I am late soon.
```

The right-side memory panel should show a priority queue, not a type-grouped archive:

```text
1. need to + verb
   repeated · practice now

2. “我来不及了”
   expression gap · review in 3 days

3. busy with work
   new · review tomorrow
```

### 3.9 Language Strategy

The product uses dynamic language switching:

```text
Chat: English-first, simple language
Fix: Chinese explanation first, English examples preserved
Drill: Chinese prompts allowed, English answers required
Memory: Chinese labels + English patterns
```

### 3.10 First-Time Experience

First-time users complete a `First Coaching Session`, not a formal placement exam.

The flow should take about 3 minutes:

```text
1. Ask the user's goal.
2. Ask daily available time.
3. Ask for 2-3 short English expression samples.
4. Give a natural correction.
5. Produce an initial profile and first memory item.
6. Start the first Today Coach session.
```

The result should weakly show CEFR level but emphasize trainable issues:

```text
Your spoken/written output looks around A2-B1.

More importantly, I noticed two things we should train first:

1. Missing "to" after want/need.
2. Direct Chinese-to-English work expressions.
```

## 4. Page Design

### 4.1 Today Coach

Components:

```text
Coach Brief
- suggested time today
- today's focus
- recent repeated issue

Conversation Stream
- coach messages
- user messages
- saved notes
- inline mini drills

Priority Memory Panel
- 3-5 highest-priority memory items
- repeated / new / due / improving state
- Start Drill / View / Snooze actions

Mode-aware Input
- Chat / Fix / Drill selector
- text input
- send action
```

### 4.2 Memory

The full Memory page is secondary. First-version sections:

```text
Error Patterns
Expression Gaps
Due Drills
Improving
```

### 4.3 Progress

Progress replaces the old dashboard emphasis. It should answer:

```text
What did I improve recently?
What keeps repeating?
What should I practice next?
How much did I practice this week?
```

The old numeric mastery stats can remain secondary.

### 4.4 Coach Review

Coach Review replaces the current weekly review experience.

First-version content:

```text
- conversation turns this week
- new memory items
- top repeated problems
- improved expressions
- next-week coaching plan
```

Text-first output is enough for v1.

## 5. Data Model Additions

Keep the existing architecture boundary:

```text
java-core    -> domain truth, memory, state, scheduling, persistence
python-agent -> LLM calls, language analysis, correction, coach phrasing
```

### 5.1 ErrorPattern

```text
id
user_id
pattern_key
label
description_zh
user_examples
better_examples
seen_count
severity
status
last_seen_at
next_drill_at
created_at
updated_at
```

### 5.2 ExpressionGap

```text
id
user_id
zh_intent
natural_expressions
user_attempts
context
seen_count
status
last_seen_at
next_drill_at
created_at
updated_at
```

### 5.3 CoachSession

```text
id
user_id
session_type: FIRST_COACHING / TODAY_COACH
started_at
ended_at
summary
detected_level_range
created_at
updated_at
```

### 5.4 CoachTurn

```text
id
session_id
mode: CHAT / FIX / DRILL
user_message
coach_message
detected_notes
created_at
```

`detected_notes` can initially be JSON, but durable facts should be merged into `ErrorPattern` and `ExpressionGap` records owned by `java-core`.

## 6. API Shape

Proposed endpoints:

```text
POST /api/coach/sessions:first
POST /api/coach/sessions
POST /api/coach/sessions/{sessionId}/turns
POST /api/coach/drills
POST /api/coach/drills/{drillId}/answer
GET  /api/memory/priority?userId={userId}
GET  /api/memory?userId={userId}
```

Example coach turn request:

```json
{
  "mode": "CHAT",
  "message": "I need prepare the demo."
}
```

Example response:

```json
{
  "coachReply": "What part of the demo feels hardest?",
  "savedNotes": [
    {
      "type": "ERROR_PATTERN",
      "key": "missing_infinitive_to",
      "label": "need to + verb",
      "userText": "I need prepare the demo.",
      "betterText": "I need to prepare the demo.",
      "severity": "MEDIUM"
    }
  ],
  "priorityMemory": [],
  "drillSuggestion": null
}
```

When the repeated pattern threshold is met:

```json
{
  "drillSuggestion": {
    "memoryType": "ERROR_PATTERN",
    "memoryId": 123,
    "prompt": "I noticed you missed 'to' again after want/need. Want to practice this for 1 minute?",
    "actions": ["START_DRILL", "NOT_NOW"]
  }
}
```

## 7. LLM Contract

`python-agent` should return structured analysis, not only natural language.

Recommended schema:

```json
{
  "coach_reply": "What part of the demo feels hardest?",
  "saved_notes": [
    {
      "type": "ERROR_PATTERN",
      "key": "missing_infinitive_to",
      "label": "need to + verb",
      "description_zh": "need/want 后面接动词时要加 to。",
      "user_text": "I need prepare the demo.",
      "better_text": "I need to prepare the demo.",
      "severity": "MEDIUM",
      "confidence": 0.9
    }
  ],
  "expression_gaps": [],
  "fix_response": null
}
```

Important boundary:

```text
LLM suggests notes.
java-core merges notes, increments counts, chooses priority, and decides drill suggestions.
```

## 8. Execution Pipeline

Each coach turn follows a stable pipeline:

```text
1. Frontend submits user input with mode.
2. java-core creates or loads CoachSession.
3. java-core calls python-agent for structured coach analysis.
4. python-agent returns coach reply and candidate notes.
5. java-core stores CoachTurn.
6. java-core merges candidate notes into Memory.
7. java-core computes priority memory.
8. java-core decides whether to suggest Drill.
9. Frontend renders reply, saved notes, memory queue, and optional drill prompt.
```

Mini drill answer pipeline:

```text
1. User starts a drill from a memory item.
2. java-core requests drill prompts from python-agent or local templates.
3. Frontend inserts Mini Drill into chat.
4. User submits answer.
5. python-agent evaluates answer and gives short feedback.
6. java-core updates memory status and next_drill_at.
7. Coach returns the user to the original conversation topic.
```

## 9. MVP Scope

In scope:

```text
First Coaching Session
Today Coach page
Chat / Fix / Drill modes
Saved notes
Priority Memory panel
ErrorPattern and ExpressionGap memory
Inline Mini Drill
Coach Review v1
```

Out of scope:

```text
voice input
pronunciation scoring
large scenario library
complex ability radar
leaderboards
gamified streak mechanics
formal 10-question placement as primary flow
large vocabulary import UX
social features
complex autonomous agent workflow
```

## 10. MVP Acceptance Criteria

The v1 experience is successful when a user can complete this loop:

```text
First visit
-> complete First Coaching Session
-> enter Today Coach
-> chat freely in English
-> receive a lightweight Saved note
-> repeat the same pattern
-> receive a 1-minute Drill suggestion
-> complete an inline Mini Drill
-> see Memory update in the right panel
-> later see the issue summarized in Coach Review
```

## 11. Suggested Implementation Order

```text
1. Add ErrorPattern / ExpressionGap / CoachSession / CoachTurn data model.
2. Add python-agent structured coach turn analysis.
3. Add java-core memory merge and priority rules.
4. Add drill suggestion rules.
5. Build Today Coach frontend.
6. Add First Coaching Session.
7. Add Coach Review v1.
8. Reposition old pages under Memory / Progress as secondary views.
```

## 12. Design Risks

### 12.1 Too Much Correction

If Chat mode corrects every sentence, the product will feel like homework.

Mitigation: in Chat mode, only save lightweight notes. Use Fix mode for detailed correction.

### 12.2 Too Little Learning Visibility

If notes are hidden only in the side panel, the product can feel like a generic chatbot.

Mitigation: show small `Saved note` chips in the conversation stream.

### 12.3 LLM Overreach

If LLM directly updates memory status or schedules drills, behavior can become inconsistent.

Mitigation: LLM only proposes structured notes. java-core owns state and scheduling.

### 12.4 MVP Scope Creep

Voice, roleplay, radar charts, gamification, and large scenario libraries are attractive but not required for the first AI-native loop.

Mitigation: validate the conversation -> memory -> drill loop first.
