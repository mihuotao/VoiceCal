package com.voicecal.model.vo;

import lombok.Data;

@Data
public class AsrVO {

    private String text;
    private double confidence;
    private long durationMs;

}
