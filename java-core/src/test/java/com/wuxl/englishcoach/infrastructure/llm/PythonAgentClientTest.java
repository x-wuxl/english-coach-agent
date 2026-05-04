package com.wuxl.englishcoach.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PythonAgentClientTest {

    private HttpServer server;
    private String receivedPath;
    private String receivedBody;
    private String receivedContentLength;
    private String receivedTransferEncoding;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void analyzeCoachTurnShouldPostToPythonAgentAndMapResponse() throws Exception {
        startServer();
        PythonAgentClient client = new PythonAgentClient("http://localhost:" + server.getAddress().getPort(), true, new ObjectMapper());

        var response = client.analyzeCoachTurn(new CoachTurnAnalysisRequest(
                "CHAT",
                "I need prepare the demo.",
                List.of(Map.of("label", "need to + verb"))
        ));

        assertThat(receivedPath).isEqualTo("/api/coach/turn/analyze");
        assertThat(receivedBody).contains("I need prepare the demo.");
        assertThat(receivedBody).contains("recent_memory");
        assertThat(receivedBody).doesNotContain("recentMemory");
        assertThat(receivedContentLength).isNotBlank();
        assertThat(receivedTransferEncoding).isNull();
        assertThat(response.coachReply()).isEqualTo("Tell me more.");
        assertThat(response.savedNotes()).hasSize(1);
        assertThat(response.savedNotes().get(0).key()).isEqualTo("missing_infinitive_to");
        assertThat(response.savedNotes().get(0).descriptionZh()).isEqualTo("need 后面接动词时要加 to。");
        assertThat(response.expressionGaps()).hasSize(1);
        assertThat(response.expressionGaps().get(0).key()).isEqualTo("demo_confidence");
        assertThat(response.expressionGaps().get(0).naturalExpressions()).containsExactly("I want to present the demo confidently.");
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/coach/turn/analyze", this::handleAnalyzeTurn);
        server.start();
    }

    private void handleAnalyzeTurn(HttpExchange exchange) throws IOException {
        receivedPath = exchange.getRequestURI().getPath();
        receivedContentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        receivedTransferEncoding = exchange.getRequestHeaders().getFirst("Transfer-Encoding");
        receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (receivedBody.isBlank()) {
            exchange.sendResponseHeaders(422, -1);
            exchange.close();
            return;
        }
        byte[] response = """
                {
                  "coach_reply": "Tell me more.",
                  "saved_notes": [{
                    "type": "ERROR_PATTERN",
                    "key": "missing_infinitive_to",
                    "label": "need to + verb",
                    "description_zh": "need 后面接动词时要加 to。",
                    "user_text": "I need prepare the demo.",
                    "better_text": "I need to prepare the demo.",
                    "severity": "MEDIUM",
                    "confidence": 0.9
                  }],
                  "expression_gaps": [{
                    "key": "demo_confidence",
                    "zh_intent": "我想更自信地演示 demo",
                    "natural_expressions": ["I want to present the demo confidently."],
                    "user_attempt": "I need prepare the demo.",
                    "context": "work",
                    "confidence": 0.8
                  }],
                  "fix_response": null
                }
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
