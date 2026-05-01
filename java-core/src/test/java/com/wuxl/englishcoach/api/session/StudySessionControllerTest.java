package com.wuxl.englishcoach.api.session;

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
class StudySessionControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:session-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:session-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
                                  "userCode": "session_user_%d",
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
    void fullSessionFlow() throws Exception {
        Long userId = createTestUser();

        // Start session
        String startResponse = mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "sessionType": "DAILY_LEARNING", "focusTheme": "daily_life"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionCode").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("STARTED"))
                .andReturn().getResponse().getContentAsString();

        // Extract session ID
        int idIdx = startResponse.indexOf("\"id\":");
        String idSub = startResponse.substring(idIdx + 5).trim();
        int idEnd = idSub.indexOf(",");
        Long sessionId = Long.parseLong(idSub.substring(0, idEnd).trim());

        // Submit attempts
        mockMvc.perform(post("/api/sessions/" + sessionId + "/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attempts": [
                                    {"learningItemId": 1, "mode": "recognition_quiz", "result": "CORRECT", "responseTimeMs": 2000, "hintUsed": false},
                                    {"learningItemId": 2, "mode": "cn_to_en", "result": "WRONG", "responseTimeMs": 8000, "hintUsed": false, "errorType": "VOCAB_CONFUSION"},
                                    {"learningItemId": 3, "mode": "recognition_quiz", "result": "CORRECT", "responseTimeMs": 3000, "hintUsed": true}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Complete session
        mockMvc.perform(post("/api/sessions/" + sessionId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fatigueFeedback": "low", "moodFeedback": "positive"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Get detail
        mockMvc.perform(get("/api/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.attempts").isArray())
                .andExpect(jsonPath("$.data.attempts.length()").value(3))
                .andExpect(jsonPath("$.data.accuracy").isNumber());
    }

    @Test
    void shouldRejectNonExistentSession() throws Exception {
        mockMvc.perform(get("/api/sessions/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4042));
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 999999, "sessionType": "DAILY_LEARNING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }
}
