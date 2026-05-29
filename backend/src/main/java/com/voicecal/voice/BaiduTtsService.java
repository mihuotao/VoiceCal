package com.voicecal.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class BaiduTtsService {

    private static final Logger log = LoggerFactory.getLogger(BaiduTtsService.class);
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String TTS_URL = "https://tsn.baidu.com/text2audio";

    private final BaiduVoiceConfig config;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpireTime;

    public BaiduTtsService(BaiduVoiceConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public byte[] synthesize(String text, String voiceType, int speed, int pitch) {
        try {
            String token = getAccessToken();
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

            String body = "tok=" + token
                    + "&tex=" + encodedText
                    + "&cuid=voicecal"
                    + "&ctp=1"
                    + "&lan=zh"
                    + "&spd=" + speed
                    + "&pit=" + pitch
                    + "&vol=5"
                    + "&per=" + getPerIndex(voiceType)
                    + "&aue=3";

            URI uri = new URI(TTS_URL);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            String contentType = conn.getContentType();

            if (contentType != null && contentType.contains("audio")) {
                return conn.getInputStream().readAllBytes();
            } else {
                byte[] errorBytes = conn.getInputStream().readAllBytes();
                String error = new String(errorBytes, StandardCharsets.UTF_8);
                log.error("TTS 合成失败: {}", error);
                throw new RuntimeException("TTS 合成失败: " + error);
            }
        } catch (Exception e) {
            log.error("TTS 服务调用异常", e);
            throw new RuntimeException("TTS 服务调用失败", e);
        }
    }

    private int getPerIndex(String voiceType) {
        if ("male".equals(voiceType)) return 1;
        if ("child".equals(voiceType)) return 3;
        return 0;
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

}
