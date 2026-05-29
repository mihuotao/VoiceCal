package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String defaultView;

    private String defaultCategory;

    private Integer defaultReminder;

    private String language;

    private Integer weekStartDay;

    private LocalTime workingHoursStart;

    private LocalTime workingHoursEnd;

    private Boolean ttsEnabled;

    private String ttsVoiceType;

    private Integer ttsSpeed;

    private Boolean notificationEnabled;

    private String theme;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
