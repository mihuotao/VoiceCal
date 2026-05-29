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
    private final BaiduAsrService asrService;
    private final VoiceNluService nluService;
    private final VoiceCommandService voiceCommandService;

    public VoiceWebSocketHandler(ObjectMapper objectMapper,
                                  BaiduAsrService asrService,
                                  VoiceNluService nluService,
                                  VoiceCommandService voiceCommandService) {
        this.objectMapper = objectMapper;
        this.asrService = asrService;
        this.nluService = nluService;
        this.voiceCommandService = voiceCommandService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText();

        switch (type) {
            case "ping" -> session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            case "audio_data" -> handleAudioData(session, json);
            default -> session.sendMessage(new TextMessage(
                    "{\"type\":\"error\",\"payload\":{\"message\":\"未知消息类型\"}}"));
        }
    }

    private void handleAudioData(WebSocketSession session, JsonNode json) throws Exception {
        String sessionId = json.get("sessionId").asText();
        String audioBase64 = json.get("payload").get("audioBase64").asText();
        String format = json.get("payload").get("format").asText("pcm");
        int sampleRate = json.get("payload").get("sampleRate").asInt(16000);

        session.sendMessage(new TextMessage(String.format(
                "{\"type\":\"intermediate_result\",\"sessionId\":\"%s\",\"payload\":{\"text\":\"识别中...\",\"isFinal\":false}}",
                sessionId)));

        BaiduAsrService.AsrResult asrResult = asrService.recognize(audioBase64, format, sampleRate);

        if (!asrResult.isSuccess()) {
            session.sendMessage(new TextMessage(String.format(
                    "{\"type\":\"error\",\"sessionId\":\"%s\",\"payload\":{\"message\":\"语音识别失败: %s\"}}",
                    sessionId, asrResult.getError())));
            return;
        }

        String recognizedText = asrResult.getText();
        session.sendMessage(new TextMessage(String.format(
                "{\"type\":\"intermediate_result\",\"sessionId\":\"%s\",\"payload\":{\"text\":\"%s\",\"isFinal\":true}}",
                sessionId, recognizedText)));

        // TODO: get userId from session token
        Long userId = 1L;
        Map<String, Object> commandResult = voiceCommandService.processTextCommand(userId, recognizedText);

        String resultJson = objectMapper.writeValueAsString(Map.of(
                "type", "final_result",
                "sessionId", sessionId,
                "payload", commandResult));

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

}
