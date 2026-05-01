package com.wuxl.englishcoach.api.mastery;

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
class MasteryControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mastery-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:mastery-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
                                  "userCode": "mastery_user_%d",
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
    void shouldQueryMasteryAfterSession() throws Exception {
        Long userId = createTestUser();

        // Start session
        String startResponse = mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "sessionType": "DAILY_LEARNING"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int idIdx = startResponse.indexOf("\"id\":");
        String idSub = startResponse.substring(idIdx + 5).trim();
        int idEnd = idSub.indexOf(",");
        Long sessionId = Long.parseLong(idSub.substring(0, idEnd).trim());

        // Submit attempts to create mastery state
        mockMvc.perform(post("/api/sessions/" + sessionId + "/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attempts": [
                                    {"learningItemId": 1, "mode": "recognition_quiz", "result": "CORRECT"},
                                    {"learningItemId": 2, "mode": "cn_to_en", "result": "WRONG"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        // Query mastery
        mockMvc.perform(get("/api/mastery")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void shouldQueryDueReview() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(get("/api/mastery/due-review")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/mastery")
                        .param("userId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }
}
