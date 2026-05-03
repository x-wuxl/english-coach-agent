package com.wuxl.englishcoach.api.memory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
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
class MemoryControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:memory-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:memory-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnPriorityMemoryWithDrillRecommendation() throws Exception {
        Long userId = createTestUser();
        jdbcTemplate.update("""
                        insert into error_pattern (user_id, pattern_key, label, description_zh, user_examples, better_examples,
                                                   seen_count, severity, status, last_seen_at, next_drill_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                "missing_infinitive_to",
                "need to + verb",
                "need takes to before a verb.",
                "[\"I need prepare the demo.\"]",
                "[\"I need to prepare the demo.\"]",
                2,
                "MEDIUM",
                "ACTIVE",
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                Timestamp.valueOf(LocalDateTime.now().minusHours(1))
        );

        mockMvc.perform(get("/api/memory/priority").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].label").value("need to + verb"))
                .andExpect(jsonPath("$.data.items[0].recommendedAction").value("START_DRILL"));
    }

    private Long createTestUser() {
        jdbcTemplate.update("""
                        insert into user_profile (user_code, goal, daily_minutes, status)
                        values (?, ?, ?, ?)
                        """,
                "memory_user_001", "GENERAL", 20, "ACTIVE");
        return jdbcTemplate.queryForObject("select id from user_profile where user_code = ?", Long.class, "memory_user_001");
    }
}
