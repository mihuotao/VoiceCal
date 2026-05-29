package com.voicecal.controller;

import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import com.voicecal.model.dto.AsrRequest;
import com.voicecal.model.dto.TtsRequest;
import com.voicecal.model.vo.AsrVO;
import com.voicecal.model.vo.TtsVO;
import com.voicecal.voice.BaiduAsrService;
import com.voicecal.voice.BaiduTtsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    private final BaiduTtsService ttsService;
    private final BaiduAsrService asrService;

    public VoiceController(BaiduTtsService ttsService, BaiduAsrService asrService) {
        this.ttsService = ttsService;
        this.asrService = asrService;
    }

    @PostMapping("/tts")
    public ApiResult<TtsVO> tts(@Valid @RequestBody TtsRequest req) {
        if (req.getText().length() > 500) {
            return ApiResult.error(ResultCode.TEXT_TOO_LONG);
        }
        try {
            String voiceType = req.getVoiceType() != null ? req.getVoiceType() : "female";
            int speed = req.getSpeed() != null ? req.getSpeed() : 5;
            int pitch = req.getPitch() != null ? req.getPitch() : 5;

            byte[] audioData = ttsService.synthesize(req.getText(), voiceType, speed, pitch);

            TtsVO vo = new TtsVO();
            vo.setAudioBase64(Base64.getEncoder().encodeToString(audioData));
            vo.setFormat("mp3");
            vo.setTextLength(req.getText().length());
            vo.setDurationMs((long) (req.getText().length() * 200));
            return ApiResult.success(vo);
        } catch (Exception e) {
            return ApiResult.error(ResultCode.TTS_SERVICE_ERROR);
        }
    }

    @PostMapping("/asr")
    public ApiResult<AsrVO> asr(@Valid @RequestBody AsrRequest req) {
        int sampleRate = req.getSampleRate() != null ? req.getSampleRate() : 16000;
        BaiduAsrService.AsrResult result = asrService.recognize(
                req.getAudioBase64(), req.getFormat(), sampleRate);

        if (!result.isSuccess()) {
            return ApiResult.error(ResultCode.ASR_SERVICE_ERROR.getCode(), result.getError());
        }

        AsrVO vo = new AsrVO();
        vo.setText(result.getText());
        vo.setConfidence(result.getConfidence());
        vo.setDurationMs(0);
        return ApiResult.success(vo);
    }

}
