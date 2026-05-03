package com.wuxl.englishcoach.api.coach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CoachReviewControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:coach-review-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:coach-review-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnCoachReview() throws Exception {
        Long userId = createTestUser();
        Long sessionId = createCoachSession(userId);
        jdbcTemplate.update("insert into coach_turn (coach_session_id, mode, user_message, coach_message, detected_notes, created_at) values (?, ?, ?, ?, ?, ?)",
                sessionId, "CHAT", "I need prepare the demo.", "Tell me more.", "[]", Timestamp.valueOf(LocalDateTime.now()));

        mockMvc.perform(get("/api/coach/review")
                        .param("userId", userId.toString())
                        .param("startDate", LocalDate.now().minusDays(2).toString())
                        .param("endDate", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationTurns").value(1))
                .andExpect(jsonPath("$.data.newMemoryCount").exists())
                .andExpect(jsonPath("$.data.topRepeatedProblems").isArray())
                .andExpect(jsonPath("$.data.improvedExpressions").isArray())
                .andExpect(jsonPath("$.data.nextWeekPlan").isNotEmpty());
    }

    private Long createTestUser() {
        jdbcTemplate.update("insert into user_profile (user_code, goal, daily_minutes, status) values (?, ?, ?, ?)",
                "coach_review_user", "GENERAL", 20, "ACTIVE");
        return jdbcTemplate.queryForObject("select id from user_profile where user_code = ?", Long.class, "coach_review_user");
    }

    private Long createCoachSession(Long userId) {
        jdbcTemplate.update("insert into coach_session (session_code, user_id, session_type, status, started_at) values (?, ?, ?, ?, ?)",
                "review_session_01", userId, "TODAY_COACH", "STARTED", Timestamp.valueOf(LocalDateTime.now()));
        return jdbcTemplate.queryForObject("select id from coach_session where session_code = ?", Long.class, "review_session_01");
    }
}
