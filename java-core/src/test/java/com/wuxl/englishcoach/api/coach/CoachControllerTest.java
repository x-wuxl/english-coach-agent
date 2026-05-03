package com.wuxl.englishcoach.api.coach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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

    private Long createTestUser(String userCode) {
        jdbcTemplate.update("""
                        insert into user_profile (user_code, goal, daily_minutes, status)
                        values (?, ?, ?, ?)
                        """,
                userCode, "GENERAL", 20, "ACTIVE");
        return jdbcTemplate.queryForObject("select id from user_profile where user_code = ?", Long.class, userCode);
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
