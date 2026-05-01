-- Seed learning items for MVP testing
-- Covers: WORD, PHRASE, SENTENCE, GRAMMAR_PATTERN across themes and difficulty levels

INSERT INTO learning_item (item_code, type, content, meaning_zh, difficulty, theme, tags, examples) VALUES

-- Daily life words
('w_001', 'WORD', 'appointment', '预约；约会', 2, 'daily_life',
 '["high_frequency", "daily_conversation"]',
 '[{"en": "I have an appointment at 3 PM.", "zh": "我下午3点有个预约。"}]'),

('w_002', 'WORD', 'convenient', '方便的；便利的', 3, 'daily_life',
 '["high_frequency", "adjective"]',
 '[{"en": "Is this time convenient for you?", "zh": "这个时间对你方便吗？"}]'),

('w_003', 'WORD', 'accommodation', '住宿；膳宿', 4, 'travel',
 '["travel_essential", "noun"]',
 '[{"en": "The accommodation was comfortable.", "zh": "住宿很舒适。"}]'),

('w_004', 'WORD', 'deadline', '截止日期', 3, 'workplace',
 '["work_essential", "noun"]',
 '[{"en": "The deadline is next Friday.", "zh": "截止日期是下周五。"}]'),

('w_005', 'WORD', 'efficient', '有效率的', 4, 'workplace',
 '["work_essential", "adjective"]',
 '[{"en": "She is very efficient at her job.", "zh": "她工作效率很高。"}]'),

-- Phrases
('p_001', 'PHRASE', 'look forward to', '期待', 3, 'daily_life',
 '["high_frequency", "verb_phrase"]',
 '[{"en": "I look forward to meeting you.", "zh": "我期待见到你。"}]'),

('p_002', 'PHRASE', 'take into account', '考虑到', 5, 'workplace',
 '["formal", "verb_phrase"]',
 '[{"en": "We need to take all factors into account.", "zh": "我们需要考虑所有因素。"}]'),

('p_003', 'PHRASE', 'get along with', '与...相处', 3, 'daily_life',
 '["daily_conversation", "verb_phrase"]',
 '[{"en": "I get along well with my colleagues.", "zh": "我和同事们相处得很好。"}]'),

('p_004', 'PHRASE', 'run out of', '用完；耗尽', 3, 'daily_life',
 '["high_frequency", "verb_phrase"]',
 '[{"en": "We have run out of milk.", "zh": "我们的牛奶用完了。"}]'),

('p_005', 'PHRASE', 'make sense', '有道理；讲得通', 4, 'daily_life',
 '["high_frequency", "verb_phrase"]',
 '[{"en": "That makes sense to me.", "zh": "那对我来说有道理。"}]'),

-- Sentences
('s_001', 'SENTENCE', 'Could you please send me the report by end of day?', '你能在今天结束前把报告发给我吗？', 5, 'workplace',
 '["email", "formal_request"]',
 '[{"en": "Could you please send me the report by end of day?", "zh": "你能在今天结束前把报告发给我吗？"}]'),

('s_002', 'SENTENCE', 'I would like to book a table for two at 7 PM.', '我想预订晚上7点的两人桌。', 4, 'daily_life',
 '["restaurant", "booking"]',
 '[{"en": "I would like to book a table for two at 7 PM.", "zh": "我想预订晚上7点的两人桌。"}]'),

('s_003', 'SENTENCE', 'The meeting has been rescheduled to next Monday.', '会议已被改期到下周一。', 5, 'workplace',
 '["meeting", "schedule_change"]',
 '[{"en": "The meeting has been rescheduled to next Monday.", "zh": "会议已被改期到下周一。"}]'),

('s_004', 'SENTENCE', 'Could you tell me how to get to the nearest subway station?', '你能告诉我怎么去最近的地铁站吗？', 3, 'travel',
 '["directions", "asking_help"]',
 '[{"en": "Could you tell me how to get to the nearest subway station?", "zh": "你能告诉我怎么去最近的地铁站吗？"}]'),

('s_005', 'SENTENCE', 'I need to reschedule our meeting due to a conflict.', '因为时间冲突，我需要重新安排我们的会议。', 5, 'workplace',
 '["meeting", "formal_request"]',
 '[{"en": "I need to reschedule our meeting due to a conflict.", "zh": "因为时间冲突，我需要重新安排我们的会议。"}]'),

-- Grammar patterns
('g_001', 'GRAMMAR_PATTERN', 'used to + verb', '过去常常...', 3, 'daily_life',
 '["grammar", "past_tense"]',
 '[{"en": "I used to play football every weekend.", "zh": "我过去每个周末都踢足球。"}]'),

('g_002', 'GRAMMAR_PATTERN', 'if + past simple, would + verb', '虚拟条件句（与现在事实相反）', 6, 'daily_life',
 '["grammar", "conditional", "subjunctive"]',
 '[{"en": "If I had more time, I would learn Japanese.", "zh": "如果我有更多时间，我会学日语。"}]'),

('g_003', 'GRAMMAR_PATTERN', 'have + past participle (present perfect)', '现在完成时', 4, 'daily_life',
 '["grammar", "present_perfect"]',
 '[{"en": "I have lived here for three years.", "zh": "我在这里住了三年了。"}]'),

('g_004', 'GRAMMAR_PATTERN', 'be going to + verb', '打算做...', 2, 'daily_life',
 '["grammar", "future", "beginner"]',
 '[{"en": "I am going to start a new project next week.", "zh": "我打算下周开始一个新项目。"}]'),

('g_005', 'GRAMMAR_PATTERN', 'would like to + verb', '想要做...（礼貌表达）', 3, 'daily_life',
 '["grammar", "polite_expression"]',
 '[{"en": "I would like to discuss this with you.", "zh": "我想和你讨论一下这个问题。"}]');
