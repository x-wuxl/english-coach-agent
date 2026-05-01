package com.wuxl.englishcoach.api.placement;

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
class PlacementControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:placement-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:placement-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
                                  "userCode": "placement_user_%d",
                                  "goal": "GENERAL",
                                  "dailyMinutes": 20
                                }
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract user ID
        int idx = response.indexOf("\"id\":");
        String sub = response.substring(idx + 5).trim();
        int end = sub.indexOf(",");
        return Long.parseLong(sub.substring(0, end).trim());
    }

    @Test
    void shouldAssessPlacement() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(post("/api/placement/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "answers": [
                                    {"section": "vocab", "questionId": "v1", "result": "CORRECT", "responseTimeMs": 2000, "hintUsed": false},
                                    {"section": "vocab", "questionId": "v2", "result": "CORRECT", "responseTimeMs": 2500, "hintUsed": false},
                                    {"section": "grammar", "questionId": "g1", "result": "CORRECT", "responseTimeMs": 3000, "hintUsed": false},
                                    {"section": "grammar", "questionId": "g2", "result": "WRONG", "responseTimeMs": 6000, "hintUsed": false},
                                    {"section": "output", "questionId": "o1", "result": "WRONG", "responseTimeMs": 15000, "hintUsed": true},
                                    {"section": "output", "questionId": "o2", "result": "WRONG", "responseTimeMs": 14000, "hintUsed": false}
                                  ]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.overallLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.vocabLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.weaknesses").isArray())
                .andExpect(jsonPath("$.data.suggestedDailyRhythm.newItems").isNumber());
    }

    @Test
    void shouldRejectEmptyAnswers() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(post("/api/placement/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "answers": []}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4094));
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(post("/api/placement/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999999,
                                  "answers": [{"section": "vocab", "questionId": "v1", "result": "CORRECT"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }
}
