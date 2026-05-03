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
class FirstCoachingSessionControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:first-coach-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:first-coach-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
        registry.add("python-agent.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCompleteFirstCoachingSessionAndReturnLevelRange() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(post("/api/coach/sessions:first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "goal": "GENERAL",
                                  "dailyMinutes": 10,
                                  "samples": ["I want improve my English because my work need it."]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detectedLevelRange").isNotEmpty())
                .andExpect(jsonPath("$.data.initialMemory.items").isArray());
    }

    private Long createTestUser() {
        jdbcTemplate.update("""
                        insert into user_profile (user_code, goal, daily_minutes, status)
                        values (?, ?, ?, ?)
                        """,
                "first_coach_user", "GENERAL", 20, "ACTIVE");
        return jdbcTemplate.queryForObject("select id from user_profile where user_code = ?", Long.class, "first_coach_user");
    }
}
