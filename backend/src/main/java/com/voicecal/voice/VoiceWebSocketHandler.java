package com.voicecal.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VoiceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public VoiceWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessions.put(session.getId(), session);
        log.info("WebSocket 连接建立: sessionId={}, userId={}", session.getId(), userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText();

        switch (type) {
            case "ping" -> {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
            case "audio_data" -> {
                handleAudioData(session, json);
            }
            default -> {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"error\",\"payload\":{\"message\":\"未知消息类型\"}}"));
            }
        }
    }

    private void handleAudioData(WebSocketSession session, JsonNode json) throws Exception {
        String sessionId = json.get("sessionId").asText();
        String audioBase64 = json.get("payload").get("audioBase64").asText();
        String format = json.get("payload").get("format").asText("pcm");
        int sampleRate = json.get("payload").get("sampleRate").asInt(16000);

        // TODO: Step 10 中将集成 BaiduAsrService 进行识别
        String resultJson = """
                {
                    "type": "final_result",
                    "sessionId": "%s",
                    "payload": {
                        "text": "（语音识别待集成）",
                        "isFinal": true
                    }
                }
                """.formatted(sessionId);

        session.sendMessage(new TextMessage(resultJson));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket 连接关闭: sessionId={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
    }

    private String extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=", 2);
                if ("token".equals(parts[0]) && parts.length > 1) {
                    return parts[1];
                }
            }
        }
        return "unknown";
    }

}
