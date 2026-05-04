package com.wuxl.englishcoach.api.coach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wuxl.englishcoach.infrastructure.llm.PythonAgentClient;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisRequest;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisResponse;
import com.wuxl.englishcoach.infrastructure.llm.dto.ExpressionGapDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class CoachControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:coach-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:coach-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
        registry.add("python-agent.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PythonAgentClient pythonAgentClient;

    @Test
    void shouldStartTodayCoachSession() throws Exception {
        Long userId = createTestUser("coach_user_001");

        mockMvc.perform(post("/api/coach/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "sessionType": "TODAY_COACH"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionType").value("TODAY_COACH"))
                .andExpect(jsonPath("$.data.sessionCode").isNotEmpty());
    }

    @Test
    void shouldSaveChatTurnAndReturnPriorityMemory() throws Exception {
        Long userId = createTestUser("coach_user_002");
        Long sessionId = startCoachSession(userId);

        mockMvc.perform(post("/api/coach/sessions/" + sessionId + "/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "CHAT", "message": "I need prepare the demo."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coachReply").isNotEmpty())
                .andExpect(jsonPath("$.data.priorityMemory.items").isArray());
    }

    @Test
    void shouldPersistExpressionGapsFromPythonAnalysis() throws Exception {
        Long userId = createTestUser("coach_user_004");
        Long sessionId = startCoachSession(userId);
        when(pythonAgentClient.analyzeCoachTurn(any())).thenReturn(new CoachTurnAnalysisResponse(
                "Try this phrasing next time.",
                Collections.emptyList(),
                List.of(new ExpressionGapDto(
                        "demo_confidence",
                        "我想更自信地演示 demo",
                        List.of("I want to present the demo confidently."),
                        "I need prepare the demo.",
                        "work",
                        0.8
                )),
                null
        ));

        mockMvc.perform(post("/api/coach/sessions/" + sessionId + "/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "CHAT", "message": "I need prepare the demo."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priorityMemory.items[0].memoryType").value("EXPRESSION_GAP"))
                .andExpect(jsonPath("$.data.priorityMemory.items[0].label").value("我想更自信地演示 demo"));
    }

    @Test
    void shouldSendLearnerContextToPythonAnalysis() throws Exception {
        Long userId = createTestUser("coach_user_context_001");
        Long sessionId = startCoachSession(userId);
        Long planId = jdbcTemplate.queryForObject("""
                select id from daily_plan_snapshot
                where user_id = ? and status = 'ACTIVE'
                order by id desc limit 1
                """, Long.class, userId);
        jdbcTemplate.update("""
                insert into daily_plan_item (daily_plan_snapshot_id, learning_item_id, item_role, sequence_no, recommended_mode)
                values (?, 1, 'NEW', 1, 'recognition_quiz')
                """, planId);
        when(pythonAgentClient.analyzeCoachTurn(any())).thenReturn(new CoachTurnAnalysisResponse(
                "Use today's plan item.",
                Collections.emptyList(),
                Collections.emptyList(),
                null
        ));

        mockMvc.perform(post("/api/coach/sessions/" + sessionId + "/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "DRILL", "message": "Practice with me."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coachReply").value("Use today's plan item."));

        ArgumentCaptor<CoachTurnAnalysisRequest> captor = ArgumentCaptor.forClass(CoachTurnAnalysisRequest.class);
        verify(pythonAgentClient).analyzeCoachTurn(captor.capture());
        CoachTurnAnalysisRequest sent = captor.getValue();
        Map<String, Object> context = sent.learnerContext();
        assert context != null;
        assert context.get("goal").equals("GENERAL");
        assert context.containsKey("todayPlanItems");
        assert context.containsKey("priorityMemory");
        assert !((List<?>) context.get("todayPlanItems")).isEmpty();
        assert sent.recentMessages() != null;
    }

    @Test
    void shouldReturnFixResponseFromPythonAnalysis() throws Exception {
        Long userId = createTestUser("coach_user_fix_001");
        Long sessionId = startCoachSession(userId);
        when(pythonAgentClient.analyzeCoachTurn(any())).thenReturn(new CoachTurnAnalysisResponse(
                "Use: I need to prepare the demo.",
                Collections.emptyList(),
                Collections.emptyList(),
                Map.of(
                        "meaning_check", "You want to say you must prepare the demo.",
                        "better_english", "I need to prepare the demo.",
                        "what_changed", List.of("Add to after need."),
                        "try_again_prompt", "Write one more sentence with need to."
                )
        ));

        mockMvc.perform(post("/api/coach/sessions/" + sessionId + "/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "FIX", "message": "I need prepare the demo."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coachReply").value("Use: I need to prepare the demo."))
                .andExpect(jsonPath("$.data.fixResponse.better_english").value("I need to prepare the demo."))
                .andExpect(jsonPath("$.data.fixResponse.what_changed[0]").value("Add to after need."));
    }

    @Test
    void shouldRejectBlankCoachTurnMessage() throws Exception {
        Long userId = createTestUser("coach_user_003");
        Long sessionId = startCoachSession(userId);

        mockMvc.perform(post("/api/coach/sessions/" + sessionId + "/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode": "CHAT", "message": "   "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("message must not be blank"));
    }

    private Long createTestUser(String userCode) {
        jdbcTemplate.update("""
                        insert into user_profile (user_code, goal, daily_minutes, overall_level, status)
                        values (?, ?, ?, ?, ?)
                        """,
                userCode, "GENERAL", 20, "A1", "ACTIVE");
        Long userId = jdbcTemplate.queryForObject("select id from user_profile where user_code = ?", Long.class, userCode);
        jdbcTemplate.update("""
                        insert into daily_plan_snapshot (plan_code, user_id, plan_date, plan_type, status, total_new_count, total_review_count, total_output_count)
                        values (?, ?, current_date, 'NORMAL', 'ACTIVE', 1, 0, 0)
                        """,
                "plan_" + userCode, userId);
        return userId;
    }

    private Long startCoachSession(Long userId) throws Exception {
        String response = mockMvc.perform(post("/api/coach/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "sessionType": "TODAY_COACH"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
