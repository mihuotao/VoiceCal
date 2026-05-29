package com.voicecal.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class BaiduAsrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduAsrService.class);
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String ASR_URL = "https://vop.baidu.com/server_api";

    private final BaiduVoiceConfig config;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpireTime;

    public BaiduAsrService(BaiduVoiceConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public AsrResult recognize(String audioBase64, String format, int sampleRate) {
        try {
            String token = getAccessToken();
            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);

            AsrRequestBody requestBody = new AsrRequestBody();
            requestBody.setFormat(format);
            requestBody.setRate(sampleRate);
            requestBody.setCuid("voicecal");
            requestBody.setChannel(1);
            requestBody.setSpeech(Base64.getEncoder().encodeToString(audioBytes));
            requestBody.setLen(audioBytes.length);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            URI uri = new URI(ASR_URL + "?access_token=" + token);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            byte[] response = conn.getInputStream().readAllBytes();
            JsonNode json = objectMapper.readTree(response);

            int errNo = json.get("err_no").asInt();
            if (errNo != 0) {
                String errMsg = json.get("err_msg").asText();
                log.error("ASR 识别失败: err_no={}, err_msg={}", errNo, errMsg);
                return new AsrResult(null, 0, errMsg);
            }

            String text = json.get("result").get(0).asText();
            double confidence = json.has("confidence") ? json.get("confidence").asDouble() : 0;
            return new AsrResult(text, confidence, null);
        } catch (Exception e) {
            log.error("ASR 服务调用异常", e);
            return new AsrResult(null, 0, "ASR 服务调用失败: " + e.getMessage());
        }
    }

    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        try {
            String url = TOKEN_URL + "?grant_type=client_credentials"
                    + "&client_id=" + config.getApiKey()
                    + "&client_secret=" + config.getSecretKey();

            URI uri = new URI(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            byte[] response = conn.getInputStream().readAllBytes();
            JsonNode json = objectMapper.readTree(response);
            accessToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt();
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;
            return accessToken;
        } catch (Exception e) {
            log.error("获取 Baidu Access Token 失败", e);
            throw new RuntimeException("获取语音服务 Token 失败", e);
        }
    }

    public static class AsrResult {
        private final String text;
        private final double confidence;
        private final String error;

        public AsrResult(String text, double confidence, String error) {
            this.text = text;
            this.confidence = confidence;
            this.error = error;
        }

        public String getText() { return text; }
        public double getConfidence() { return confidence; }
        public String getError() { return error; }
        public boolean isSuccess() { return error == null && text != null; }
    }

    @SuppressWarnings("unused")
    private static class AsrRequestBody {
        private String format;
        private int rate;
        private String cuid;
        private int channel;
        private String speech;
        private int len;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public int getRate() { return rate; }
        public void setRate(int rate) { this.rate = rate; }
        public String getCuid() { return cuid; }
        public void setCuid(String cuid) { this.cuid = cuid; }
        public int getChannel() { return channel; }
        public void setChannel(int channel) { this.channel = channel; }
        public String getSpeech() { return speech; }
        public void setSpeech(String speech) { this.speech = speech; }
        public int getLen() { return len; }
        public void setLen(int len) { this.len = len; }
    }

}
