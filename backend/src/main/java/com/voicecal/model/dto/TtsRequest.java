package com.voicecal.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TtsRequest {

    @NotBlank(message = "文本不能为空")
    private String text;

    private String voiceType;

    private Integer speed;

    private Integer pitch;

}
