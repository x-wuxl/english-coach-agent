package com.wuxl.englishcoach.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisRequest;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PythonAgentClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAgentClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public PythonAgentClient(@Value("${python-agent.base-url:http://localhost:8000}") String baseUrl,
                              @Value("${python-agent.enabled:true}") boolean enabled,
                              ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    /**
     * Request error explanation from python-agent.
     * Returns null if python-agent is unavailable or returns an error.
     */
    public String explainError(String itemContent, String meaningZh, String userAnswer,
                                String mode, String errorType) {
        if (!enabled) return null;

        try {
            Map<String, Object> body = Map.of(
                    "item_content", itemContent,
                    "meaning_zh", meaningZh,
                    "mode", mode,
                    "user_answer", userAnswer != null ? userAnswer : "",
                    "error_type", errorType != null ? errorType : ""
            );

            Map response = restClient.post()
                    .uri("/api/explain/error")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Failed to call python-agent for explanation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Request coach feedback from python-agent.
     * Returns null if python-agent is unavailable.
     */
    public Map<String, String> getCoachFeedback(String itemContent, String meaningZh,
                                                 String userAnswer, String result,
                                                 String mode, String errorType, boolean hintUsed) {
        if (!enabled) return null;

        try {
            Map<String, Object> body = Map.of(
                    "item_content", itemContent,
                    "meaning_zh", meaningZh,
                    "result", result,
                    "mode", mode,
                    "user_answer", userAnswer != null ? userAnswer : "",
                    "error_type", errorType != null ? errorType : "",
                    "hint_used", hintUsed
            );

            Map response = restClient.post()
                    .uri("/api/coach/feedback")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return response;
        } catch (Exception e) {
            log.warn("Failed to call python-agent for coach feedback: {}", e.getMessage());
            return null;
        }
    }

    public CoachTurnAnalysisResponse analyzeCoachTurn(CoachTurnAnalysisRequest request) {
        if (!enabled) return null;

        try {
            return restClient.post()
                    .uri("/api/coach/turn/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CoachTurnAnalysisResponse.class);
        } catch (Exception e) {
            log.warn("Failed to analyze coach turn via python-agent: {}", e.getMessage());
            return null;
        }
    }
}
