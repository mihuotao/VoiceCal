package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("voice_command_log")
public class VoiceCommandLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sessionId;

    private String rawAudioText;

    private String intent;

    private String entities;

    private BigDecimal confidence;

    private String nluSource;

    private String commandResult;

    private String responseText;

    private Integer durationMs;

    private String errorMessage;

    private LocalDateTime createdAt;

}
