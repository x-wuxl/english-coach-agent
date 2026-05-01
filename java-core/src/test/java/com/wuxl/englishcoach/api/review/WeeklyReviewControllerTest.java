package com.wuxl.englishcoach.api.review;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WeeklyReviewControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:weeklyreview-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:weeklyreview-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    private Long createTestUser() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userCode": "review_user_%d",
                                  "goal": "GENERAL",
                                  "dailyMinutes": 20
                                }
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int idx = response.indexOf("\"id\":");
        String sub = response.substring(idx + 5).trim();
        int end = sub.indexOf(",");
        return Long.parseLong(sub.substring(0, end).trim());
    }

    @Test
    void shouldGenerateWeeklyReview() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(post("/api/reviews/weekly:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "weekStartDate": "2026-04-28",
                                  "weekEndDate": "2026-05-04"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.completionRate").isNumber())
                .andExpect(jsonPath("$.data.studyMinutes").isNumber())
                .andExpect(jsonPath("$.data.highFrequencyErrorTypes").isArray())
                .andExpect(jsonPath("$.data.strongestThemes").isArray())
                .andExpect(jsonPath("$.data.weakestThemes").isArray())
                .andExpect(jsonPath("$.data.nextWeekSuggestion").isNotEmpty());
    }

    @Test
    void shouldRejectDuplicateReview() throws Exception {
        Long userId = createTestUser();

        // First generation
        mockMvc.perform(post("/api/reviews/weekly:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "weekStartDate": "2026-04-21",
                                  "weekEndDate": "2026-04-27"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Duplicate
        mockMvc.perform(post("/api/reviews/weekly:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "weekStartDate": "2026-04-21",
                                  "weekEndDate": "2026-04-27"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4092));
    }

    @Test
    void shouldGetExistingReview() throws Exception {
        Long userId = createTestUser();

        // Generate
        mockMvc.perform(post("/api/reviews/weekly:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "weekStartDate": "2026-04-14",
                                  "weekEndDate": "2026-04-20"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Query back
        mockMvc.perform(get("/api/reviews/weekly")
                        .param("userId", userId.toString())
                        .param("weekStartDate", "2026-04-14")
                        .param("weekEndDate", "2026-04-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.weekStartDate").value("2026-04-14"));
    }

    @Test
    void shouldReturn404ForMissingReview() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(get("/api/reviews/weekly")
                        .param("userId", userId.toString())
                        .param("weekStartDate", "2099-01-01")
                        .param("weekEndDate", "2099-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4045));
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(post("/api/reviews/weekly:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999999,
                                  "weekStartDate": "2026-04-28",
                                  "weekEndDate": "2026-05-04"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }
}
