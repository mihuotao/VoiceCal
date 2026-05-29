package com.voicecal.model.vo;

import lombok.Data;

@Data
public class TtsVO {

    private String audioBase64;
    private String format;
    private long durationMs;
    private int textLength;

}
