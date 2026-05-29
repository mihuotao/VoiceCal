package com.voicecal.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateReminderRequest {

    @NotNull(message = "提醒时间不能为空")
    private LocalDateTime remindAt;

    private Integer remindMinutesBefore;

    private String method;

}
