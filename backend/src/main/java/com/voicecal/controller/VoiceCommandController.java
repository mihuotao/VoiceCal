package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.entity.VoiceCommandLog;
import com.voicecal.service.VoiceCommandLogService;
import com.voicecal.voice.VoiceCommandService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/voice")
public class VoiceCommandController {

    private final VoiceCommandService voiceCommandService;
    private final VoiceCommandLogService voiceCommandLogService;

    public VoiceCommandController(VoiceCommandService voiceCommandService,
                                   VoiceCommandLogService voiceCommandLogService) {
        this.voiceCommandService = voiceCommandService;
        this.voiceCommandLogService = voiceCommandLogService;
    }

    @PostMapping("/command")
    public ApiResult<?> processCommand(@CurrentUser LoginUser loginUser,
                                        @Valid @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ApiResult.badRequest("语音文本不能为空");
        }
        var result = voiceCommandService.processTextCommand(loginUser.getUserId(), text);
        return ApiResult.success(result);
    }

    @PostMapping("/festival-greeting")
    public ApiResult<Map<String, Object>> festivalGreeting(@CurrentUser LoginUser loginUser,
                                                            @RequestBody Map<String, Object> body) {
        return voiceCommandService.handleFestivalGreeting(loginUser.getUserId(), body);
    }

}
