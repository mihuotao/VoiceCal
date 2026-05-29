package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.entity.VoiceCommandLog;
import com.voicecal.service.VoiceCommandLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/voice-logs")
public class VoiceLogController {

    private final VoiceCommandLogService voiceCommandLogService;

    public VoiceLogController(VoiceCommandLogService voiceCommandLogService) {
        this.voiceCommandLogService = voiceCommandLogService;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@CurrentUser LoginUser loginUser,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String intent,
                                                      @RequestParam(required = false) String result) {
        var pageResult = voiceCommandLogService.listByQuery(
                loginUser.getUserId(), intent, result, page, size);

        List<Map<String, Object>> list = pageResult.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ApiResult.page(list, pageResult.getTotal(), page, size);
    }

    private Map<String, Object> toMap(VoiceCommandLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("rawAudioText", log.getRawAudioText());
        map.put("intent", log.getIntent());
        map.put("confidence", log.getConfidence());
        map.put("nluSource", log.getNluSource());
        map.put("commandResult", log.getCommandResult());
        map.put("responseText", log.getResponseText());
        map.put("durationMs", log.getDurationMs());
        map.put("createdAt", log.getCreatedAt() != null
                ? log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return map;
    }

}
