package com.wuxl.englishcoach.api.progress;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ProgressControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:progress-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:progress-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
        registry.add("python-agent.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void summaryReturnsRealMasterySessionAndWeakItemCounts() throws Exception {
        Long userId = createUser();
        jdbcTemplate.update("""
                insert into mastery_state (user_id, learning_item_id, seen_count, correct_count, wrong_count, correct_streak,
                    recognition_score, recall_score, output_score, memory_strength, forget_risk, status, next_review_at, created_at, updated_at)
                values (?, 1, 3, 2, 1, 1, ?, ?, ?, ?, ?, 'LEARNING', ?, ?, ?)
                """, userId, BigDecimal.valueOf(0.4), BigDecimal.ZERO, BigDecimal.valueOf(0.2), BigDecimal.ZERO,
                BigDecimal.valueOf(0.7), LocalDateTime.now().minusHours(1), LocalDateTime.now(), LocalDateTime.now());
        jdbcTemplate.update("""
                insert into study_session (session_code, user_id, session_date, session_type, status, accuracy, completion_rate, completed_at, created_at, updated_at)
                values ('progress_session_1', ?, ?, 'DAILY_LEARNING', 'COMPLETED', ?, ?, ?, ?, ?)
                """, userId, LocalDate.now(), BigDecimal.valueOf(0.67), BigDecimal.ONE, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        jdbcTemplate.update("""
                insert into attempt_log (attempt_code, user_id, learning_item_id, study_session_id, mode, result, response_text, response_time_ms, hint_used, occurred_at, created_at)
                values ('progress_attempt_1', ?, 1, (select id from study_session where session_code = 'progress_session_1'), 'cn_to_en', 'WRONG', 'prep demo', 2000, false, ?, ?)
                """, userId, LocalDateTime.now(), LocalDateTime.now());

        mockMvc.perform(get("/api/progress/summary")
                        .param("userId", String.valueOf(userId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalMasteryItems").value(1))
                .andExpect(jsonPath("$.data.dueReviewCount").value(1))
                .andExpect(jsonPath("$.data.completedSessionsThisWeek").value(1))
                .andExpect(jsonPath("$.data.recentAccuracy").value(0.67))
                .andExpect(jsonPath("$.data.topWeakItems[0].learningItemId").value(1));
    }

    private Long createUser() {
        jdbcTemplate.update("""
                insert into user_profile (user_code, goal, daily_minutes, status) values ('progress_user_1', 'GENERAL', 20, 'ACTIVE')
                """);
        return jdbcTemplate.queryForObject("select id from user_profile where user_code = 'progress_user_1'", Long.class);
    }
}
