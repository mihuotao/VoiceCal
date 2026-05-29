package com.voicecal.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsrRequest {

    @NotBlank(message = "音频数据不能为空")
    private String audioBase64;

    @NotBlank(message = "音频格式不能为空")
    private String format;

    private Integer sampleRate;

}
