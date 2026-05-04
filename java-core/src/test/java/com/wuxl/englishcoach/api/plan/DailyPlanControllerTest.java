package com.wuxl.englishcoach.api.plan;

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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class DailyPlanControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:dailyplan-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:dailyplan-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
                                  "userCode": "plan_user_%d",
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
    void shouldGenerateDailyPlan() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(post("/api/plans/daily:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "planDate": "2026-05-01",
                                  "planType": "NORMAL"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planCode").isNotEmpty())
                .andExpect(jsonPath("$.data.planDate").value("2026-05-01"))
                .andExpect(jsonPath("$.data.planType").value("NORMAL"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.newItems").isArray())
                .andExpect(jsonPath("$.data.reviewItems").isArray())
                .andExpect(jsonPath("$.data.rationale.loadDecision").isNotEmpty());
    }

    @Test
    void shouldRejectDuplicatePlan() throws Exception {
        Long userId = createTestUser();

        // First generation
        mockMvc.perform(post("/api/plans/daily:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "planDate": "2026-05-02",
                                  "planType": "NORMAL"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Duplicate
        mockMvc.perform(post("/api/plans/daily:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "planDate": "2026-05-02",
                                  "planType": "NORMAL"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4092));
    }

    @Test
    void shouldEnsureDailyPlanByCreatingWhenMissing() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(post("/api/plans/daily:ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "planDate": "2026-05-04",
                                  "planType": "NORMAL"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planCode").isNotEmpty())
                .andExpect(jsonPath("$.data.planDate").value("2026-05-04"))
                .andExpect(jsonPath("$.data.planType").value("NORMAL"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.newItems[0].itemId").isNumber())
                .andExpect(jsonPath("$.data.newItems[0].itemCode").isNotEmpty())
                .andExpect(jsonPath("$.data.newItems[0].type").isNotEmpty())
                .andExpect(jsonPath("$.data.newItems[0].content").isNotEmpty())
                .andExpect(jsonPath("$.data.newItems[0].meaningZh").exists())
                .andExpect(jsonPath("$.data.newItems[0].difficulty").isNumber())
                .andExpect(jsonPath("$.data.newItems[0].theme").isNotEmpty())
                .andExpect(jsonPath("$.data.newItems[0].examples").isArray())
                .andExpect(jsonPath("$.data.newItems[0].itemRole").value("NEW"));
    }

    @Test
    void shouldEnsureDailyPlanIdempotently() throws Exception {
        Long userId = createTestUser();
        String request = """
                {
                  "userId": %d,
                  "planDate": "2026-05-05",
                  "planType": "NORMAL"
                }
                """.formatted(userId);

        MvcResult first = mockMvc.perform(post("/api/plans/daily:ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String firstPlanCode = first.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"planCode\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/plans/daily:ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planCode").value(firstPlanCode))
                .andExpect(jsonPath("$.data.newItems[0].itemId").isNumber());
    }

    @Test
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(post("/api/plans/daily:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999999,
                                  "planDate": "2026-05-01",
                                  "planType": "NORMAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }

    @Test
    void shouldGetExistingPlan() throws Exception {
        Long userId = createTestUser();

        // Generate first
        mockMvc.perform(post("/api/plans/daily:generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "planDate": "2026-05-03",
                                  "planType": "NORMAL"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Query it back
        mockMvc.perform(get("/api/plans/daily")
                        .param("userId", userId.toString())
                        .param("planDate", "2026-05-03")
                        .param("planType", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.planDate").value("2026-05-03"))
                .andExpect(jsonPath("$.data.planType").value("NORMAL"));
    }

    @Test
    void shouldReturn404ForMissingPlan() throws Exception {
        Long userId = createTestUser();

        mockMvc.perform(get("/api/plans/daily")
                        .param("userId", userId.toString())
                        .param("planDate", "2099-01-01")
                        .param("planType", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4043));
    }
}
