package com.voicecal.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class BaiduStreamAsrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduStreamAsrService.class);
    private static final String WS_URL = "wss://vop.baidu.com/realtime_asr";

    private final BaiduVoiceConfig config;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocket> websockets = new ConcurrentHashMap<>();
    private final Map<String, Consumer<AsrStreamResult>> callbacks = new ConcurrentHashMap<>();
    private final OkHttpClient httpClient;

    public BaiduStreamAsrService(BaiduVoiceConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public CompletableFuture<String> startSession(Consumer<AsrStreamResult> callback) {
        String sessionId = UUID.randomUUID().toString();
        callbacks.put(sessionId, callback);
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            String sn = "voicecal-" + sessionId.substring(0, 8);
            String url = WS_URL + "?sn=" + sn;
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    websockets.put(sessionId, webSocket);
                    log.info("Baidu ASR 流式连接建立: sessionId={}, httpStatus={}", sessionId, response.code());
                    sendStartFrame(webSocket, sessionId);
                    future.complete(sessionId);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.info("Baidu ASR 收到文本帧: sessionId={}, data={}", sessionId, text);
                    handleMessage(sessionId, text);
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    websockets.remove(sessionId);
                    log.info("Baidu ASR 流式连接关闭中: sessionId={}, code={}, reason={}", sessionId, code, reason);
                    webSocket.close(1000, null);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    websockets.remove(sessionId);
                    log.info("Baidu ASR 流式连接已关闭: sessionId={}, code={}, reason={}", sessionId, code, reason);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("Baidu ASR 流式连接失败: sessionId={}, msg={}", sessionId, t.getMessage(), t);
                    websockets.remove(sessionId);
                    callbacks.remove(sessionId);
                    future.completeExceptionally(t);
                }
            });

        } catch (Exception e) {
            log.error("创建 Baidu ASR 流式会话失败", e);
            callbacks.remove(sessionId);
            future.completeExceptionally(e);
        }

        return future;
    }

    private void sendStartFrame(WebSocket webSocket, String sessionId) {
        try {
            Map<String, Object> startFrame = Map.of(
                    "type", "START",
                    "data", Map.of(
                            "appid", Integer.parseInt(config.getAppId()),
                            "appkey", config.getApiKey(),
                            "dev_pid", 15372,
                            "cuid", "voicecal-" + sessionId.substring(0, 8),
                            "format", "pcm",
                            "sample", 16000
                    )
            );
            String json = objectMapper.writeValueAsString(startFrame);
            webSocket.send(json);
            log.info("Baidu ASR START 帧已发送: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("发送 START 帧失败", e);
        }
    }

    public void sendAudio(String sessionId, byte[] pcmData) {
        WebSocket ws = websockets.get(sessionId);
        if (ws != null) {
            log.info("转发音频到 Baidu: sessionId={}, size={}bytes", sessionId, pcmData.length);
            ws.send(ByteString.of(pcmData));
        } else {
            log.warn("Baidu WebSocket 不可用，丢弃音频: sessionId={}, size={}bytes", sessionId, pcmData.length);
        }
    }

    public void endSession(String sessionId) {
        WebSocket ws = websockets.remove(sessionId);
        if (ws != null) {
            try {
                ws.send("{\"type\":\"FINISH\"}");
                log.info("Baidu ASR FINISH 帧已发送: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("发送 FINISH 帧失败", e);
            }
        }
    }

    public void cancelSession(String sessionId) {
        WebSocket ws = websockets.remove(sessionId);
        callbacks.remove(sessionId);
        if (ws != null) {
            try {
                ws.close(1000, "cancel");
            } catch (Exception e) {
                log.error("取消会话失败", e);
            }
        }
    }

    private void handleMessage(String sessionId, String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String type = json.has("type") ? json.get("type").asText() : "";

            Consumer<AsrStreamResult> callback = callbacks.get(sessionId);
            if (callback == null) {
                log.warn("Baidu ASR 消息无回调: sessionId={}, type={}, message={}", sessionId, type, message);
                return;
            }

            switch (type) {
                case "MID_TEXT" -> {
                    String result = json.has("result") ? json.get("result").asText() : "";
                    callback.accept(new AsrStreamResult(result, false, 0, 0, null));
                }
                case "FIN_TEXT" -> {
                    int errNo = json.has("err_no") ? json.get("err_no").asInt() : -1;
                    if (errNo == 0) {
                        String result = json.has("result") ? json.get("result").asText() : "";
                        long startTime = json.has("start_time") ? json.get("start_time").asLong() : 0;
                        long endTime = json.has("end_time") ? json.get("end_time").asLong() : 0;
                        callback.accept(new AsrStreamResult(result, true, startTime, endTime, null));
                    } else {
                        String errMsg = json.has("err_msg") ? json.get("err_msg").asText() : "未知错误";
                        log.warn("Baidu ASR FIN_TEXT 错误: err_no={}, err_msg={}", errNo, errMsg);
                        callback.accept(new AsrStreamResult(null, true, 0, 0, errMsg));
                    }
                }
                case "HEARTBEAT" -> {
                    // ignore
                }
                default -> log.debug("Baidu ASR 未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("解析 Baidu ASR 消息失败", e);
        }
    }

    public static class AsrStreamResult {
        private final String text;
        private final boolean isFinal;
        private final long startTime;
        private final long endTime;
        private final String error;

        public AsrStreamResult(String text, boolean isFinal, long startTime, long endTime, String error) {
            this.text = text;
            this.isFinal = isFinal;
            this.startTime = startTime;
            this.endTime = endTime;
            this.error = error;
        }

        public String getText() { return text; }
        public boolean isFinal() { return isFinal; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getError() { return error; }
        public boolean isSuccess() { return error == null && text != null; }
    }
}
