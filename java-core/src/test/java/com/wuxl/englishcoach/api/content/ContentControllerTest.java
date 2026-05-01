package com.wuxl.englishcoach.api.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContentControllerTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:content-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.url", () -> "jdbc:h2:mem:content-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.flyway.user", () -> "sa");
        registry.add("spring.flyway.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListLearningItems() throws Exception {
        mockMvc.perform(get("/api/content/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(20));
    }

    @Test
    void shouldFilterByType() throws Exception {
        mockMvc.perform(get("/api/content/items").param("type", "WORD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].type").value("WORD"));
    }

    @Test
    void shouldFilterByDifficultyRange() throws Exception {
        mockMvc.perform(get("/api/content/items")
                        .param("difficultyMin", "4")
                        .param("difficultyMax", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void shouldGetItemDetail() throws Exception {
        // First get an item ID from the list
        String response = mockMvc.perform(get("/api/content/items").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract first item ID
        Long itemId = extractFirstItemId(response);

        mockMvc.perform(get("/api/content/items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.itemCode").isNotEmpty())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.examples").isArray());
    }

    @Test
    void shouldReturn404ForMissingItem() throws Exception {
        mockMvc.perform(get("/api/content/items/{itemId}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4044));
    }

    private Long extractFirstItemId(String json) {
        // Simple extraction - find first "id" value
        int idx = json.indexOf("\"id\":");
        if (idx < 0) return 0L;
        String sub = json.substring(idx + 5).trim();
        int end = sub.indexOf(",");
        if (end < 0) end = sub.indexOf("}");
        return Long.parseLong(sub.substring(0, end).trim());
    }
}
