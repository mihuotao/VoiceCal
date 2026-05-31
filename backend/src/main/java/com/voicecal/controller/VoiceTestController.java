package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.voice.LlmNluService;
import com.voicecal.voice.NluResult;
import com.voicecal.voice.VoiceCommandService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 语音测试控制器 - 仅在 dev 环境启用
 * 用于测试语音命令功能，跳过 ASR 识别
 */
@RestController
@RequestMapping("/api/v1/voice/test")
@Profile("dev")
public class VoiceTestController {

    private final VoiceCommandService voiceCommandService;
    private final LlmNluService llmNluService;

    public VoiceTestController(VoiceCommandService voiceCommandService,
                                LlmNluService llmNluService) {
        this.voiceCommandService = voiceCommandService;
        this.llmNluService = llmNluService;
    }

    /**
     * 测试语音命令 - 直接传入文本，跳过 ASR
     */
    @PostMapping("/command")
    public ApiResult<?> testCommand(@CurrentUser LoginUser loginUser,
                                     @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ApiResult.badRequest("语音文本不能为空");
        }
        var result = voiceCommandService.processTextCommand(loginUser.getUserId(), text);
        return ApiResult.success(result);
    }

    /**
     * 提取实体 - 用户已选择意图，LLM 只提取实体
     */
    @PostMapping("/extract")
    public ApiResult<?> extractEntities(@RequestBody Map<String, String> body) {
        String intent = body.get("intent");
        String text = body.get("text");

        if (intent == null || intent.isBlank()) {
            return ApiResult.badRequest("意图不能为空");
        }
        if (text == null || text.isBlank()) {
            return ApiResult.badRequest("语音文本不能为空");
        }

        // 调用 LLM 只提取实体（不判断意图）
        NluResult result = llmNluService.extractEntities(intent, text);
        return ApiResult.success(result);
    }
}
