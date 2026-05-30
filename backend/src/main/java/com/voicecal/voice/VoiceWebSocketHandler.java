package com.voicecal.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicecal.auth.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class VoiceWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> baiduSessionMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingBaiduSessions = new ConcurrentHashMap<>();
    private final Map<String, List<byte[]>> pendingAudioBuffers = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdMap = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> legacyBuffers = new ConcurrentHashMap<>();
    private final Map<String, Long> userIdMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final BaiduStreamAsrService streamAsrService;
    private final BaiduAsrService asrService;
    private final VoiceCommandService voiceCommandService;
    private final JwtProvider jwtProvider;

    public VoiceWebSocketHandler(ObjectMapper objectMapper,
                                  BaiduStreamAsrService streamAsrService,
                                  BaiduAsrService asrService,
                                  VoiceCommandService voiceCommandService,
                                  JwtProvider jwtProvider) {
        this.objectMapper = objectMapper;
        this.streamAsrService = streamAsrService;
        this.asrService = asrService;
        this.voiceCommandService = voiceCommandService;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);

        // 从 URI 参数获取 token 并解析用户 ID
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            log.info("WebSocket URI query: {}", query);
            if (query != null) {
                var params = UriComponentsBuilder.fromUriString("?" + query).build().getQueryParams();
                String token = params.getFirst("token");
                log.info("WebSocket token length: {}", token != null ? token.length() : 0);
                if (token != null && !token.isBlank()) {
                    Long userId = jwtProvider.getUserIdFromToken(token);
                    userIdMap.put(session.getId(), userId);
                    log.info("WebSocket 用户认证成功: sessionId={}, userId={}", session.getId(), userId);
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket 用户认证失败: sessionId={}, error={}, class={}", session.getId(), e.getMessage(), e.getClass().getName());
        }

        log.info("前端 WebSocket 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText();

        switch (type) {
            case "ping" -> sendJson(session, Map.of("type", "pong"));
            case "start" -> handleStart(session, json);
            case "end" -> handleEnd(session, json);
            case "audio_data" -> handleLegacyAudioData(session, json);
            default -> sendJson(session, Map.of(
                    "type", "error",
                    "payload", Map.of("message", "未知消息类型: " + type)));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String wsId = session.getId();
        int size = message.getPayload().remaining();
        byte[] pcmBytes = new byte[size];
        message.getPayload().get(pcmBytes);

        String baiduSid = baiduSessionMap.get(wsId);
        if (baiduSid != null) {
            streamAsrService.sendAudio(baiduSid, pcmBytes);
            return;
        }

        // Baidu 连接未就绪，缓冲音频帧
        List<byte[]> buffer = pendingAudioBuffers.get(wsId);
        if (buffer != null) {
            if (buffer.size() < 300) {
                buffer.add(pcmBytes);
            } else {
                log.warn("音频缓冲区已满，丢弃帧: wsSession={}, size={}", wsId, size);
            }
        } else {
            log.warn("无 Baidu 会话，丢弃音频帧: wsSession={}, size={}", wsId, size);
        }
    }

    private void handleStart(WebSocketSession session, JsonNode json) {
        String sessionId = json.has("sessionId") ? json.get("sessionId").asText() : session.getId();
        String wsId = session.getId();
        sessionIdMap.put(wsId, sessionId);

        // 初始化音频缓冲区，Baidu 就绪后自动刷新
        pendingAudioBuffers.put(wsId, new ArrayList<>());

        CompletableFuture<String> baiduFuture = streamAsrService.startSession(result -> {
            try {
                if (result.isSuccess()) {
                    if (!result.isFinal()) {
                        sendJson(session, Map.of(
                                "type", "intermediate_result",
                                "sessionId", sessionId,
                                "payload", Map.of("text", result.getText(), "isFinal", false)));
                    } else {
                        sendJson(session, Map.of(
                                "type", "intermediate_result",
                                "sessionId", sessionId,
                                "payload", Map.of("text", result.getText(), "isFinal", true)));

                        Long userId = userIdMap.getOrDefault(wsId, 1L);
                        Map<String, Object> commandResult = voiceCommandService.processTextCommand(userId, result.getText());
                        sendJson(session, Map.of(
                                "type", "final_result",
                                "sessionId", sessionId,
                                "payload", commandResult));
                    }
                } else {
                    sendJson(session, Map.of(
                            "type", "error",
                            "sessionId", sessionId,
                            "payload", Map.of("message", "语音识别失败: " + result.getError())));
                }
            } catch (Exception e) {
                log.error("处理 ASR 结果失败", e);
            }
        });

        // 10s 超时，不阻塞 handleStart
        CompletableFuture<String> timedFuture = baiduFuture.orTimeout(10, TimeUnit.SECONDS);

        timedFuture.whenComplete((baiduSid, throwable) -> {
            pendingBaiduSessions.remove(wsId);

            if (throwable != null) {
                log.warn("Baidu 流式 ASR 连接失败: wsSession={}, type={}, msg={}",
                        wsId, throwable.getClass().getName(), throwable.getMessage());
                pendingAudioBuffers.remove(wsId);

                String msg;
                if (throwable instanceof java.util.concurrent.TimeoutException) {
                    msg = "语音服务连接超时，请检查网络或 Baidu 配置";
                } else {
                    msg = "语音服务连接失败: " + throwable.getClass().getSimpleName()
                            + " - " + (throwable.getMessage() != null ? throwable.getMessage() : "未知");
                }
                sendJson(session, Map.of(
                        "type", "error",
                        "sessionId", sessionId,
                        "payload", Map.of("message", msg)));
            } else {
                baiduSessionMap.put(wsId, baiduSid);
                log.info("Baidu 流式 ASR 会话就绪: wsSession={}, baiduSession={}", wsId, baiduSid);

                // 刷新缓冲音频帧
                List<byte[]> buffer = pendingAudioBuffers.remove(wsId);
                if (buffer != null && !buffer.isEmpty()) {
                    log.info("刷新已缓冲音频数据: wsSession={}, frames={}", wsId, buffer.size());
                    for (byte[] pcm : buffer) {
                        streamAsrService.sendAudio(baiduSid, pcm);
                    }
                }

                sendJson(session, Map.of(
                        "type", "intermediate_result",
                        "sessionId", sessionId,
                        "payload", Map.of("text", "准备就绪，请说话...", "isFinal", false)));
            }
        });
    }

    private void handleEnd(WebSocketSession session, JsonNode json) throws Exception {
        String wsId = session.getId();
        String sessionId = sessionIdMap.getOrDefault(wsId, wsId);
        pendingAudioBuffers.remove(wsId);

        // 取消仍在连接中的 Baidu 会话
        CompletableFuture<String> pending = pendingBaiduSessions.remove(wsId);
        if (pending != null) {
            pending.cancel(true);
        }

        String baiduSid = baiduSessionMap.remove(wsId);

        if (baiduSid != null) {
            streamAsrService.endSession(baiduSid);
            log.info("Baidu 流式 ASR 会话结束: sessionId={}", sessionId);
        } else {
            log.warn("Baidu 会话不存在，发送降级错误: sessionId={}", sessionId);
            sendJson(session, Map.of(
                    "type", "error",
                    "sessionId", sessionId,
                    "payload", Map.of("message", "语音识别未就绪")));
        }
    }

    private void handleLegacyAudioData(WebSocketSession session, JsonNode json) throws Exception {
        String sessionId = json.get("sessionId").asText();
        String audioBase64 = json.get("payload").get("audioBase64").asText();
        String format = json.get("payload").get("format").asText("pcm");
        int sampleRate = json.get("payload").get("sampleRate").asInt(16000);

        sendJson(session, Map.of(
                "type", "intermediate_result",
                "sessionId", sessionId,
                "payload", Map.of("text", "识别中...", "isFinal", false)));

        BaiduAsrService.AsrResult asrResult = asrService.recognize(audioBase64, format, sampleRate);

        if (!asrResult.isSuccess()) {
            sendJson(session, Map.of(
                    "type", "error",
                    "sessionId", sessionId,
                    "payload", Map.of("message", "语音识别失败: " + asrResult.getError())));
            return;
        }

        String recognizedText = asrResult.getText();
        sendJson(session, Map.of(
                "type", "intermediate_result",
                "sessionId", sessionId,
                "payload", Map.of("text", recognizedText, "isFinal", true)));

        Long userId = userIdMap.getOrDefault(session.getId(), 1L);
        Map<String, Object> commandResult = voiceCommandService.processTextCommand(userId, recognizedText);
        sendJson(session, Map.of(
                "type", "final_result",
                "sessionId", sessionId,
                "payload", commandResult));
    }

    private void sendJson(WebSocketSession session, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String wsId = session.getId();
        String baiduSid = baiduSessionMap.remove(wsId);
        if (baiduSid != null) {
            streamAsrService.cancelSession(baiduSid);
        }
        CompletableFuture<String> pending = pendingBaiduSessions.remove(wsId);
        if (pending != null) {
            pending.cancel(true);
        }
        pendingAudioBuffers.remove(wsId);
        sessions.remove(wsId);
        sessionIdMap.remove(wsId);
        legacyBuffers.remove(wsId);
        userIdMap.remove(wsId);
        log.info("前端 WebSocket 连接关闭: sessionId={}", wsId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
    }
}
