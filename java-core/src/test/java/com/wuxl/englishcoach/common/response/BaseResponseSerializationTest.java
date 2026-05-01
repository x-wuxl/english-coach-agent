package com.wuxl.englishcoach.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.common.config.JacksonConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class BaseResponseSerializationTest {

    private final ObjectMapper objectMapper = buildObjectMapper();

    @Test
    void shouldSerializeSuccessResponseWithStableFields() throws Exception {
        String json = objectMapper.writeValueAsString(
                BaseResponse.success(new Payload("alma", LocalDateTime.of(2026, 3, 26, 12, 30, 0))));

        assertThat(json).contains("\"code\":0");
        assertThat(json).contains("\"message\":\"ok\"");
        assertThat(json).contains("\"name\":\"alma\"");
        assertThat(json).contains("\"createdAt\":\"2026-03-26T12:30:00\"");
    }

    @Test
    void shouldSerializePageResponsePayload() throws Exception {
        PageResponse<String> pageResponse = new PageResponse<>(List.of("a", "b"), 1, 20, 2L, 1);

        String json = objectMapper.writeValueAsString(BaseResponse.success(pageResponse));

        assertThat(json).contains("\"items\":[\"a\",\"b\"]");
        assertThat(json).contains("\"page\":1");
        assertThat(json).contains("\"total\":2");
    }

    private ObjectMapper buildObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        Jackson2ObjectMapperBuilderCustomizer customizer = new JacksonConfig().jackson2ObjectMapperBuilderCustomizer();
        customizer.customize(builder);
        return builder.build();
    }

    private record Payload(String name, LocalDateTime createdAt) {
    }
}
